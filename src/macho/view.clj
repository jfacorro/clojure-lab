(ns macho.view
  (:require [macho.ui :as ui]))

(defn validate-ops [ops]
  (let [ops (->> ops (map first) set)]
    (when (nil? ops)
      (throw (Exception. "Must define at least the 'create' op.")))
    (when (ops 'this)
      ; Check there's no definition for a "this" operation.
      (throw (Exception. "Can't define a 'this' op.")))
      ; Check there's a definitino for the "create" operation.
    (when-not (ops 'create)
      (throw (Exception. "Must define a 'create' op.")))))

(defmacro defview
  "Defines a view with the operations defined in ops."
  [name & ops]
    (validate-ops ops)
    (let [create  (->> ops (filter #(= 'create (first %))) (drop 2))
          ops     (->> ops
                       (mapcat (juxt first #(concat '(fn) %)))
                       (apply assoc {}))
          create  (ops 'create)
          ops     (dissoc ops 'create)
          ops-map (reduce #(assoc %1 (keyword %2) %2) {} (keys ops))]
      `(let [~'this (~create)
             ~@(mapcat identity ops)
             ops#  ~ops-map]
        (def ~name
          (fn [op# & args#]
            (let [v# (ops# op#)]
              (cond (nil? v#)
                      (throw (Error. (str "No such operation: " (name op#))))
                    (fn? v#)
                      (apply v# args#)
                    :else
                      v#)))))))

(defview document-view
  (create []
    {:text (javax.swing.JTextPane.)
     :history-index 0})
  (update [doc-ref old-doc new-doc]
    (let [idx     (:history-index this)
          ; Get the history of operations on the document from idx onwards.
          hst     (doc/history doc-ref idx)
          ; Get the history of operations on the :ast alternate model from idx onwards.
          ast-hst (doc/history (doc/alternate doc-ref :ast) idx)]
      (process-doc-history (:text this) hst)
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
            v))))
))
