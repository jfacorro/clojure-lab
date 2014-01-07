(ns lab.model.document
  "A document is the representation in memory of the file, which
holds not only a buffer for the string but additional information
(e.g. the language, the parse tree or any useful information 
that may need to be computed or mantained)."
  (:refer-clojure :exclude [replace name])
  (:require [lab.model [buffer :as b]
                       [history :as h]]
            [lab.util :as util]
            [clojure.java.io :as io]))

(declare modified? delete insert text)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; History

(defn- archive-operations
  "Add a list of operations in the history."
  [doc ops]
  (update-in doc [:history] h/add ops))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Editing operations and their inverse

(defrecord InsertText [offset s]
  h/Bijection
  (direct [this]
    #(insert % offset s))
  (inverse [this]
    #(delete % offset (+ offset (count s)))))

(defrecord DeleteText [start end s]
  h/Bijection
  (direct [this]
    #(delete % start end))
  (inverse [this]
    #(insert % start s)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Document Record

(defrecord Document [name path modified buffer lang])

(defn- default-buffer
  "Returns a buffer implementation."
  ([lang]
    (default-buffer lang ""))
  ([lang s]
    (b/incremental-buffer lang s)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; IO operations

(defn- remove-lf-cr
  "Removes all Windows new line chars and replaces 
them with *nix new line."
  [txt]
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Properties 

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

(defn lang [doc]
  (:lang doc))

(defn parse-tree [doc]
  (:parse-tree doc))

(defn line-count
  "Returns the number of lines in the document's content."
  [doc]
  (->> (text doc)
    (filter #{\newline})
    count
    (+ 2)))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Text operations

(defn insert
  "Inserts s at the document's offset position.
  Returns the document."
  [doc offset s]
  (let [ops [(->InsertText offset s)]]
    (-> doc
      (update-in [:buffer] b/insert offset s)
      (assoc-in [:modified] true)
      (archive-operations ops))))

(defn append
  "Appends s to the document's content.
  Returns the document."
  [doc s]
  (insert doc (length doc) s))

(defn delete
  "Deletes the document's content from start to end position.
  Returns the modified document."
  [doc start end]
  (let [s   (.substring ^String (text doc) start end)
        ops [(->DeleteText start end s)]]
    (-> doc
      (archive-operations ops)
      (update-in [:buffer] b/delete start end)
      (assoc-in [:modified] true))))

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
                            :modified false
                            :lang     lang
                            :buffer   (default-buffer lang)
                            :history  (h/history)})]
    (if path
      (bind doc path)
      doc)))
