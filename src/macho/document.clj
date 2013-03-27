(ns macho.document
  (:refer-clojure :exclude [replace find]))

(defprotocol Text
  (append [this s])
  (insert [this offset s])
  (delete [this start end])
  (length [this])
  (search [this s])
  (replace [this search repl]))
  
(defprotocol Openable
  (open [this]))

(defrecord Document [path]
  Openable
  (open [this]
    (let [text (if (:path this)
                 (StringBuffer. ^String (slurp path))
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

(defn make-doc
  "Creates a new document using the name, path 
and alternate models provided."
  [name & [path alts]]
  (assoc (Document. name)
         :path path
         :alternates alts))

(comment
  (defn on-append [k r old-val new-val]
    (println :on-append ":" old-val "->" new-val " - Yo!:" r))
  
  (def doc (atom (with-meta (Document. nil) {:doc true}) :meta {:atom true}))
  (add-watch doc :on-append on-append)
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