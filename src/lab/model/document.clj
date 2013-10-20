(ns lab.model.document
  (:refer-clojure :exclude [replace name])
  (:require [lab.model [buffer :as b]
                       [history :as h]]
            [lab.util :as util]
            [clojure.java.io :as io]))

(declare modified? delete insert text)

;; History

(defn- record-operations
  "Add a list of operations in the history."
  [doc ops]
  (update-in doc [:history] h/add ops))

;; Document operations

(defrecord InsertText [offset s])
(defrecord DeleteText [start end s])

(extend-protocol h/Bijection
  InsertText
  (direct [this]
    (let [offset (:offset this)
          s      (:s this)]
      #(insert % offset s)))
  (inverse [this]
    (let [offset (:offset this)
          n      (-> this :s count)]
      #(delete % offset (+ offset n))))

  DeleteText
  (direct [this]
    (let [start (:start this)
          end   (:end this)]
      #(delete % start end)))
  (inverse [this]
    (let [start (:start this)
          s     (:s this)]
      #(insert % start s))))

;; Document

(defrecord Document [name path modified buffer alternates])

(defn- default-buffer
  "Returns a buffer implementation."
  [& xs]
  (apply b/incremental-buffer xs))

;; IO operations

(defn bind
  "Binds a document to a file in the given path. If the 
file exists then the contents are read into the document's
buffer."
  [doc path & {:keys [new?]}]
  (let [file   (io/file path)
        buf    (if new?
                 (:buffer doc)
                 (default-buffer (if (.exists file) (-> path slurp) "")))
        name   (.getName file)
        props  {:buffer   buf
                :path     path
                :modified (boolean new?)
                :name     name}]
    (merge doc props)))

(defn close
  "Closes a file checking if its been modified first."
  [doc]
  (if (modified? doc)
    (throw (RuntimeException. "Sorry, can't close a modified document."))))

(defn save
  "Saves the document to a file in path."
  [{:keys [path modified] :as doc}]
  (assert path "The document doesn't have a path.")
  (if modified
    (do
      (spit path (text doc))
      (assoc doc :modified false))
    doc))

;; Properties

(defn name
  "Returns the document's name."
  [doc]
  (:name doc))

(defn length
  "Returns the document content's length."
  [doc]
  (b/length (:buffer doc)))

(defn text
  "Returns the document's content."
  [doc]
  (b/text (:buffer doc)))

(defn path
  "Returns the path for the binded file if any."
  [doc]
  (:path doc))

(defn file
  "If the document is bound to a file, then an instance
  of this file is returned, otherwise returns nil."
  [doc]
  (io/file (path doc)))

(defn modified?
  "Returns true if the document was modified since 
  it was created, opened or the last time it was saved."
  [doc]
  (:modified doc))

(defn search
  "Find the matches for the expression in the document
  and returns the delimiters (index start and end) for each
  match in ascending order."
  [doc s]
  (util/find-limits s (text doc)))

;; Text operations

(defn insert
  "Inserts s at the document's offset position.
  Returns the document."
  [doc offset s]
  (let [ops [(->InsertText offset s)]]
    (-> doc
      (update-in [:buffer] b/insert offset s)
      (assoc-in [:modified] true)
      (record-operations ops))))

(defn append
  "Appends s to the document's content.
  Returns the document."
  [doc s]
  (insert doc (length doc) s))

(defn delete
  "Deletes the document's content from start to end position.
  Returns the modified document."
  [doc start end]
  (let [s   (-> doc text (.substring start end))
        ops [(->DeleteText start end s)]]
    (-> doc
      (record-operations ops)
      (update-in [:buffer] b/delete start end)
      (assoc-in [:modified] true))))

(defn replace
  "Replaces all ocurrences of src with rpl."
  [doc src rpl]
  (let [limits (->> (search doc src) (sort-by first >))
        f      (fn [x [s e]]
                 (h/with-no-history (-> x (delete s e) (insert s rpl))))
        g      (fn [[s e]] [(->DeleteText s e src) (->InsertText s rpl)])
        ops    (mapcat g limits)]
    (-> (reduce f doc limits)
      (record-operations ops))))

(def ^:dynamic *untitled-count* (atom 0))

(defn- untitled
  "Returns a name for a new document."
  []
  (swap! *untitled-count* inc) 
  (str "Untitled " @*untitled-count*))

;; Document creation function

(defn document
  "Creates a new document using the name."
  [& {:keys [path] :or {path nil}}]
  (let [doc (map->Document {:name (untitled)
                            :path nil
                            :modified false
                            :buffer (default-buffer)
                            :history (h/history)})]
    (if path
      (bind doc path)
      doc)))
