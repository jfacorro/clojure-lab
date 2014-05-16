(ns lab.model.document
  "A document is the representation in memory of the file, which
holds not only a buffer for the string but additional information
(e.g. the language, the parse tree or any useful information 
that may need to be computed or mantained)."
  (:refer-clojure :exclude [replace name])
  (:require [lab.model [buffer :as b]
                       [history :as h]
                       [protocols :as p]]
            [lab.util :as util]
            [clojure.java.io :as io]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; History

(def ^{:private true :dynamic true} *bundle* nil)

(defn- current-bundle [doc]
  (-> doc :history h/current meta ::bundle))

(defn- archive-operations
  "Add a list of operations in the history."
  [doc ops]
  (cond
    (and *bundle* (= *bundle* (current-bundle doc)))
      (update-in doc [:history] h/update into ops)
    *bundle*
      (update-in doc [:history] h/add (with-meta ops {::bundle *bundle*}))
    :else
      (update-in doc [:history] h/add ops)))

(defmacro bundle-operations
  "Creates a context in which all operations done to a document
  are bundled in a sinlge operation in its history.
  When two o more bundlings are nested, only the first onr is 
  considered."
  [& body]
  `(if *bundle*
     ~@body
     (binding [*bundle* (gensym "bundle-")]
       ~@body)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Undoable protocol

(defrecord InsertText [offset s]
  h/Undoable
  (h/redo [this]
    #(p/insert % offset s))
  (h/undo [this]
    #(p/delete % offset (+ offset (count s)))))

(defrecord DeleteText [start end s]
  h/Undoable
  (h/redo [this]
    #(p/delete % start end))
  (h/undo [this]
    #(p/insert % start s)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Document Undo/Redo

(defn undo
  ([x]
    (let [[x hist] (undo x (:history x))]
      (assoc x :history hist)))
  ([x hist]
    (let [ops     (h/current hist)
          hist    (h/rewind hist)
          inv-ops (->> ops (map h/undo) reverse)]
      (h/with-no-history
        [(reduce #(%2 %) x inv-ops) hist]))))

(defn redo 
  ([x]
    (let [[x hist] (redo x (:history x))]
      (assoc x :history hist)))
  ([x old-hist]
    (let [hist     (h/forward old-hist)
          ops      (when (not= hist old-hist)
                     (h/current hist))
          inv-ops  (map h/redo ops)]
      (h/with-no-history
        [(reduce #(%2 %) x inv-ops) hist]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Default buffer implementation

(defn- default-buffer
  "Returns a buffer implementation."
  ([lang]
    (default-buffer lang ""))
  ([lang s]
    (b/incremental-buffer lang s)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Document Record

(def ^{:private true :dynamic true} *no-op* false)

(defmacro no-op
  "Any text editing operations (insertor delete) done to any document
  in the body are a no op (i.e. the document's text is unaltered)."
  [& body]
  `(binding [*no-op* true]
    ~@body))

(defmacro ^:private with-no-op [doc & body]
  `(if *no-op*
     ~doc
     ~@body))

(defrecord Document [name path modified? buffer lang]
  p/Text
  (insert [this offset s]
    (with-no-op this
      (let [ops [(->InsertText offset s)]]
        (-> this
          (update-in [:buffer] p/insert offset s)
          (assoc-in [:modified?] true)
          (archive-operations ops)))))
  (append [this s]
    (p/insert this (p/length this) s))
  (delete [this start end]
    (with-no-op this
      (let [s   (p/substring buffer start end)
            ops [(->DeleteText start end s)]]
        (-> this
          (archive-operations ops)
          (update-in [:buffer] p/delete start end)
          (assoc-in [:modified?] true)))))
  (length [this]
    (p/length buffer))
  (text [this]
    (p/text buffer))
  (substring [this start end]
    (p/substring buffer start end))

  p/Parsable
  (parse-tree [this]
    (p/parse-tree buffer)))

(def insert #'p/insert)
(def delete #'p/delete)
(def length #'p/length)
(def text #'p/text)
(def substring #'p/substring)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; IO operations

(defn- remove-lf-cr
  "Removes all Windows new line chars and replaces 
them with *nix new line."
  [^String txt]
  (.replace txt "\r\n" "\n"))

(defn bind
  "Binds a document to a file in the given path. If the 
file exists then the contents are read into the document's
buffer."
  [doc path & {:keys [new?]}]
  (let [file   (io/file path)
        buf    (if new?
                 (:buffer doc)
                 (default-buffer (:lang doc)
                                 (if (.exists file) (-> path slurp remove-lf-cr) "")))
        name   (.getName file)
        props  {:buffer   buf
                :path     path
                :modified? (boolean new?)
                :name     name}]
    (merge doc props)))

(defn close
  "Closes a file checking if its been modified first."
  [doc]
  (if (:modified? doc)
    (throw (RuntimeException. "Sorry, can't close a modified document."))))

(defn save
  "Saves the document to a file in path."
  [{:keys [path modified?] :as doc}]
  (assert path "The document doesn't have a path.")
  (if modified?
    (do
      (spit path (text doc))
      (assoc doc :modified? false))
    doc))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Text operations

(defn append
  "Appends s to the document's content.
  Returns the document."
  [doc s]
  (insert doc (p/length doc) s))

(defn search
  "Find the matches for the expression in the document
  and returns the delimiters (index start and end) for each
  match in ascending order."
  [doc s]
  (util/find-limits s (text doc)))

(defn replace
  "Replaces all ocurrences of src with rpl."
  [doc src rpl]
  (let [limits (->> (search doc src) (sort-by first >))
        f      (fn [x [s e]]
                 (h/with-no-history (-> x (delete s e) (insert s rpl))))
        g      (fn [[s e]] [(->DeleteText s e src) (->InsertText s rpl)])
        ops    (mapcat g limits)]
    (-> (reduce f doc limits)
      (archive-operations ops))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; New documents name generation

(def ^:dynamic *untitled-count* (atom 0))

(defn- untitled!
  "Returns a name for a new document."
  []
  (swap! *untitled-count* inc) 
  (str "Untitled " @*untitled-count*))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Creation function

(defn document
  "Creates a new document using the name."
  [lang & [path]]
  (let [doc (map->Document {:name     (when-not path (untitled!))
                            :path     nil
                            :modified? false
                            :lang     lang
                            :buffer   (default-buffer lang)
                            :history  (h/history)})]
    (if path
      (bind doc path)
      doc)))
