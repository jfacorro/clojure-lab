(ns macho.view)

(defn validate-ops [ops-map]
  (let [ops (-> ops-map keys set)]
    (when-let [msg (or (and (nil? ops) "Must define at least the 'create' op.")
                       ; Check there's no definition for a "this" operation.
                       (and (ops :this) "Can't define a 'this' op.")
                       ; Check there's a definitino for the "create" operation.
                       (and (not (ops :create)) "Must define a 'create' op."))]
      (throw (Exception. msg)))))

(defmacro defview
  "Defines a view with the operations defined in ops using defview*.
  The view contains a closure on a local binding called 'this', whose value is
  the return value from the :create function in the ops-map."
  [name & ops]
    (let [ops-map  (->> (or ops '())
                        (reduce #(assoc %1 (-> %2 first keyword ) (concat '(fn) %2)) {}))]
      `(let [create# ~(:create ops-map)
             ~'this  (if (fn? create#) (create#) create#)]
        (def ~name (defview* ~ops-map)))))

(defn defview*
  "Returns a view, that's actually a function which receives
  a keyword operation (from the operations map) and the arguments to
  this operation."
  [ops-map]
  (validate-ops ops-map)
  (let [ops-map (dissoc ops-map :create)]
    (fn [op & args]
      (let [v (ops-map op)]
        (cond (nil? v)
          (throw (Error. (str "No such operation: " (name op))))
        (fn? v)
          (apply v args)
        :else
          v)))))

#_(defview document-view
  (create []
    {:text (javax.swing.JTextPane.)
     :history-index 0})
  (update [doc-ref old-doc new-doc]
    (let [idx     (:history-index this)
          ; Get the history of operations on the document from idx onwards.
          hst     (doc/history doc-ref idx)
          ; Get the history of operations on the :ast alternate model from idx onwards.
          ast-hst (doc/history (doc/alternate doc-ref :ast) idx)]
      ; Update the text in the view based on the changes done to the document.
      (process-doc-history (:text this) hst)
      ; Update syntax highlight using incremental differences from the ast.
      (process-ast-history (:text this) ast-hst))))

(comment
  ;; Macro-definition
  (defview document-view  
    (create []
      (JTextPane.))
    (init [doc]
      (ui/set this :text (text doc)))
    (update [atom-ref old-doc new-doc]
      (ui/set this :text (text doc))))
  ;; Macro-expansion
  (let [create (fn create [] (JTextPane.))
        ,,,
        this   (create)
        ops    {:create create :init init :view this}]
    (def name
      (fn [op & args]
        (let [v (ops op)]
          (if (fn? v)
            (apply v args)
            v)))))
)
