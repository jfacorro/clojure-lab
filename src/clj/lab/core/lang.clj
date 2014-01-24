(ns lab.core.lang
  "Languages are used to determine the structure and syntax of a Document.
They also provide the functions to create and update a parse tree."
  (:require [lab.model.protocols :as p]
            [clojure.zip :as zip]))

;;;;;;;;;;;;;;;;;;;;;;
;; Languages resolution and predicates

(defn resolve-lang
  "Based on the langs and path provided, it uses the :rank 
function from each lang to determine the appropiate one."
  [path langs default]
  (let [[rank lang] (->> langs
                      (map #(vector ((:rank %) path) %))
                      (sort-by first)
                      reverse
                      first)]
    (if (zero? rank) default lang)))

(defn file-extension?
  "Returns a positive number if the path's extension 
equals ext, zero otherwise."
  [ext ^String path]
  (if (-> path (.split "\\.") last (= ext))
    1
    0))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Language 
;;
;; Holds the information for parsing each language.
;; Its fields are self-explanatory except for lang?, which 
;; should hold a predicate function that recieves a doc and
;; returns true if the doc is of that language.

(defrecord Language [name options grammar lang? styles])

(def ^{:dynamic true} *node-group*
  "Determines the value that will be assigned to the nodes 
created during the node generation when creating the parse
tree.")

(defn node-group
  "Returns the name of the group this node belongs to."
  [node]
  (-> node meta :group))

(defn node-meta
  "If the tag for the node is a symbol
check if its one of the registered symbols."
  [tag content]
  {:style tag :group *node-group*})

(defn calculate-length [content]
  (reduce #(+ %1 (if (string? %2) (.length %2) (:length %2))) 0 content))

(defn make-node [tag content]
  (with-meta {:tag tag :length (calculate-length content) :content content}
             (node-meta tag content)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Plain Text Lang

(def plain-text
  (map->Language
    {:name     "Plain text"
     :options  {:main      :expr*
                :root-tag  ::root
                :make-node #'make-node}
     :grammar  [:expr #"[\s\S]+"]
     :rank     (partial file-extension? "txt")
     :styles   {:default {:color 0xFFFFFF}
                :expr    {:color 0xFFFFFF}}}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Parsing

(defn parse-tree
  "Parses the incremental buffer of a Document and returns the
parse tree with the modified nodes marked with node-group."
  ([doc]
    (parse-tree doc nil))
  ([doc node-group]
    (binding [*node-group* node-group]
      (p/parse-tree doc))))

(defn- tag
  "If the node is a map returns its :tag, otherwise the keyword :default."
  [node]
  (or (and (map? node) (node :tag)) :default))

(defn code-zip
  "Builds a zipper using the root node in the document
under the :parse-tree key."
  [root]
  (zip/zipper map? :content make-node root))

(defn- node-length
  "Returns the length of a node in the parse tree."
  [node]
  (cond (nil? node) 
          0
        (string? node)
          (.length ^String node)
        :else
          (:length node)))

(defn offset
  "Finds the offset of the given zipper location
by going right and up adding the lenghts of each
node in the way to the root."
  [loc]
  (loop [loc loc, n 0]
    (if-not loc
      n
      (recur (zip/up loc)
             (->> (zip/lefts loc)
               (map node-length)
               (apply + n))))))

(defn location
  "Finds the location that contains the offset,
returns a vector with the location and the 
offset of the begging of it."
  [root offset]
  (loop [loc (zip/down root), pos 0]
    (when (and loc (not (zip/end? loc)))
      (let [node    (zip/node loc)
            len     (node-length node)
            new-pos (+ pos len)]
        (if (and (string? node) (<= pos offset))
          (if (< offset new-pos)
            [loc pos]
            (recur (zip/next loc) new-pos))
          (if (< new-pos offset)
            (recur (zip/right loc) new-pos)
            (recur (zip/down loc) pos)))))))

(def ^:private ignore? #{:whitespace})

(defn- next-no-down
  "Finds the next zipper location that's not a children.
This is used by tokens* as part of a performance enhancement,
which avoids visiting children of a node that is not in the
generated node-group.

Source code taken from clojure.zip/next function."
  [loc]
    (if (zip/end? loc)
      loc
      (or 
       (zip/right loc)
       (loop [p loc]
         (if (zip/up p)
           (or (zip/right (zip/up p))
               (recur (zip/up p)))
           [(zip/node p) :end])))))

(defn- tokens*
  "Gets the limits for each string in the tree, ignoring
the limits for the nodes with the tag specified by ignore?.
If group is false all tokens are returned, otherwise
only the tokens from the last tree generation are returned."
  [loc group]
  (loop [loc (zip/next loc), offset 0, limits (transient [])]
    (let [node   (zip/node loc)
          length (node-length node)]
      (cond (string? node)
              (let [new-offset (+ offset length)
                    parent     (-> loc zip/up zip/node)
                    tag        (tag parent)
                    {:keys [style group]} (meta parent)
                    limits     (if (and group
                                        (or (ignore? tag) (not= group group)))
                                 limits
                                 (conj! limits [offset length style]))]
                (recur (zip/next loc) new-offset limits))
            (zip/end? loc)
              (persistent! limits)
            :else
              (if (or (nil? node)
                      (nil? group)
                      (and group (= (group node) group)))
                (recur (zip/next loc) offset limits)
                (recur (next-no-down loc) (+ offset length) limits))))))

(defn tokens
  "Returns the tokens identified incrementally in the parse-tree 
generation that used the group-id identifier provided."
  [root group]
  (tokens* (code-zip root) group))
