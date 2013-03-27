(ns macho.document
  (:refer-clojure :exclude [replace]))

(defprotocol Text
  (append [this s])
  (insert [this offset s])
  (delete [this start end])
  (length [this])
  (search [this s])
  (replace [this search repl]))
  
(defprotocol Openable
  (open [this]))

(defrecord Document [name]
  Openable
  (open [this]
    (let [text (if (:path this)
                 (StringBuffer. ^String (slurp (:path this)))
                 (StringBuffer.))]
      (assoc this :text text)))
  Text
  (append [this s]
    (println :append-doc)
    (append (:text this) s)
    this)
  (insert [this offset s] 
    (insert (:text this) offset s)
    this)
  (length [this]
    (length (:text this))
    this))

(extend-type StringBuffer
  Text
  (append [this s]
    (println :append-buffer)
    (.append this s))
  (insert [this offset s] 
    (.insert ^:StringBuffer this ^int offset ^String s))
  (length [this]
    (.length this)))

(defmacro ! [f at & args]
  `(swap! ~at ~f ~@args))

(defn make-document
  "Creates a new document using the name, path 
and alternate models provided."
  [name & [path alts]]
  (assoc (Document. name)
         :path path
         :alternates alts))

(defn attach-view
  "Attaches a view to the document. doc should be 
  an agent/atom/var/ref reference."
  [doc view]
  (view :init doc)
  (add-watch doc :update view))

(comment
  ;; Usage from control
  (let [doc     (make-document "bla")
        view    (default-document-view)
        control (default-document-control)]
    (attach-view doc view)
    (attach-control doc view control)
    (add-document doc)
    (add-view main-view view))
  
  ;; Handle state change from the view
  (fn [op & args]
    (case op
      :init (apply init args)
      :update (apply update args)
      ,,,))
  ;;-----------------------------------------
  (def doc (atom (with-meta (Document. nil) {:doc true}) :meta {:atom true}))
  (println (meta doc) (meta @doc))
  
  (! open doc)
  (! append doc "bla")
  (! append doc " ")
  (! append doc "ble")
  
  (defn alternate [entity f]
    (if (instance? clojure.lang.Atom entity)
      nil))
    
  (alternate doc nil)
  
  (meta #'append)
)