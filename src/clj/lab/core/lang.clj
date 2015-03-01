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
  "Returns a 1 if the path's extension equals ext
  and 0 otherwise."
  [ext ^String path]
  (if (= ext (-> path (.split "\\.") last))
    1
    0))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Language
;;
;; Holds the information for parsing each language.
;; Its fields are self-explanatory except for :rank, which 
;; should hold a function that recieves a path and
;; returns a number between 0 and 1.

(defrecord Language [id name options grammar rank styles])

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
  (reduce #(+ %1 (if (string? %2) (.length ^String %2) (:length %2))) 0 content))

(defn make-node [tag content]
  (with-meta {:tag tag :length (calculate-length content) :content content}
             (node-meta tag content)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Plain Text Lang

(def plain-text
  (map->Language
    {:id       :plain-text
     :name     "Plain text"
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
parse tree with the modified nodes marked with node group-id."
  ([doc]
    (parse-tree doc (gensym "group-")))
  ([doc group-id]
    (binding [*node-group* group-id]
      (p/parse-tree doc))))

(defn- tag
  "If the node is a map returns its :tag, otherwise the keyword :default."
  [node]
  (or (and (map? node) (node :tag)) :default))

(defn parent-node
  "Returns the parent tree node for the provided location."
  [loc]
  (-> loc zip/up zip/node))

(defn code-zip
  "Builds a zipper using the root node in the document
under the :parse-tree key."
  [root-node]
  (zip/zipper map? :content make-node root-node))

(defn node-length
  "Returns the length of a node in the parse tree."
  [node]
  (cond (nil? node) 
          0
        (string? node)
          (.length ^String node)
        :else
          (:length node)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Location Helpers

(defn location-tag
  "Returns the tag for the location if any."
  [loc]
  (loop [loc loc]
    (when loc
      (if-not (map? (zip/node loc))
        (recur (zip/up loc))
        (:tag (zip/node loc))))))

(defn location-length
  "Returns the length of the node tree for loc."
  [loc]
  (if loc
    (-> loc zip/node node-length)
    0))

(defn loc-string?
  "Returns true if the zipper location
contains a string and false otherwise."
  [loc]
  (and loc (-> loc zip/node string?)))

(defn select-location
  "Returns the first location that satisifes the predicate
  moving in the direction specified. If no match is found
  returns nil."
  [loc dir p]
  (if (or (nil? loc) (p loc))
    loc
    (recur (dir loc) dir p)))

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

(defn limits
  "Returns the start and end offset for the location."
  [loc]
  (let [start (offset loc)
        end   (+ start (-> loc zip/node node-length))]
    [start end]))

(defn location
  "Finds the location that contains the offset,
  returns a vector with the location and its start
  offset."
  [root-loc offset]
  ; Check the bounds for offset
  (when-let [root-length (and (<= 0 offset)
                              (<= offset (node-length (zip/node root-loc)))
                              (node-length (zip/node root-loc)))]
    (loop [loc (zip/down root-loc), pos 0]
      (when (and loc (not (zip/end? loc)))
        (let [node    (zip/node loc)
              new-pos (+ pos (node-length node))]
          ; If node is a string and the position we're at
          ; is less than the offset we're looking for
          (if (and (string? node) (<= pos offset))
            ; Check if the new positions will go past the offset.
            ; If it won't or the offset is actually the same as the 
            ; lenght, then the current location is the one we are 
            ; looking for.
            (if (or (< offset new-pos)
                    (and (= offset new-pos root-length)))
              [loc pos]
              (recur (zip/next loc) new-pos))
            (if (< new-pos offset)
              (recur (zip/right loc) new-pos)
              (recur (zip/down loc) pos))))))))

(defn whitespace?
  "Returns true if the zipper location
contains a node that represents whitespace and
false otherwise."
  [loc]
  (and loc (-> loc location-tag #{:whitespace} boolean)))

(def ^:private ignore? #{:whitespace})

(defn search
  "Takes a location and a predicate, iterating depth first
  through all nodes. Returning a list of locations that
  satisfy the predicate."
  [loc pred]
  (loop [loc loc
         res []]
    (if (zip/end? loc)
      res
      (recur (zip/next loc)
             (if (pred loc) (conj res loc) res)))))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Token search

(defn- tokens*
  "Gets the limits for each string in the tree, ignoring
the limits for the nodes with the tag specified by ignore?.
If group is false all tokens are returned, otherwise
only the tokens from the last tree generation are returned."
  [loc group-id]
  (loop [loc (zip/next loc), offset 0, limits (transient [])]
    (let [node   (zip/node loc)
          length (node-length node)]
      (cond (string? node)
              (let [new-offset (+ offset length)
                    parent     (-> loc zip/up zip/node)
                    tag        (tag parent)
                    {:keys [style group]} (meta parent)
                    limits     (if (and group-id
                                        (or (ignore? tag) (not= group group-id)))
                                 limits
                                 (conj! limits [offset length style]))]
                (recur (zip/next loc) new-offset limits))
            (zip/end? loc)
              (persistent! limits)
            :else
              (if (or (nil? node)
                      (nil? group-id)
                      (and group-id (= (node-group node) group-id)))
                (recur (zip/next loc) offset limits)
                (recur (next-no-down loc) (+ offset length) limits))))))

(defn tokens
  "Returns the tokens identified incrementally in the parse-tree 
generation that used the group-id identifier provided."
  [root group-id]
  (tokens* (code-zip root) group-id))
