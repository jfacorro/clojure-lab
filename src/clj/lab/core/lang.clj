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

(defn node-meta
  "If the tag for the node is a symbol
check if its one of the registered symbols."
  [tag content]
  {:style tag :group *node-group*})

(defn length [content]
  (reduce #(+ %1 (if (string? %2) (.length %2) (:length %2))) 0 content))

(defn make-node [tag content]
  (with-meta {:tag tag :length (length content) :content content}
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
  [doc node-group]
  (binding [*node-group* node-group]
    (p/parse-tree doc)))

(defn- tag
  "If the node is a map returns its :tag, otherwise the keyword :default."
  [node]
  (or (and (map? node) (node :tag)) :default))

(defn- code-zip
  "Builds a zipper using the root node in the document
under the :parse-tree key."
  [root]
  (zip/zipper map? :content make-node root))

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
If node-group is false all tokens are returned, otherwise
only the tokens from the last tree generation are returned."
  [loc node-group]
  (loop [loc (zip/next loc), offset 0, limits (transient [])]
    (let [node (zip/node loc)]
      (cond (string? node)
              (let [length     (.length ^String node)
                    new-offset (+ offset length)
                    parent     (-> loc zip/up zip/node)
                    tag        (tag parent)
                    {:keys [style group]} (meta parent)
                    limits     (if (and node-group
                                        (or (ignore? tag) (not= group node-group)))
                                 limits
                                 (conj! limits [offset length style]))]
                (recur (zip/next loc) new-offset limits))
            (zip/end? loc)
              (persistent! limits)
            :else
              (if (or (nil? node)
                      (nil? node-group)
                      (and node-group (= (-> node meta :group) node-group)))
                (recur (zip/next loc) offset limits)
                (recur (next-no-down loc) (+ offset (:length node)) limits))))))

(defn tokens
  "Returns the tokens identified incrementally in the parse-tree 
generation that used the group-id identifier provided."
  [root node-group]
  (tokens* (code-zip root) node-group))

;;;;;;;;;;;;;;;;;;;;;;;
;; Definitions

(defn offset [node]
  (loop [node node
         offset 0]
    (if-not node
      offset
      (let [x  (zip/node node)]
        (recur (zip/prev node)
               (if (string? x)
                 (+ offset (.length x))
                 offset))))))

(defn definitions [lang root]
  (let [{:keys [def? node->def]}  lang
        node (zip/down (code-zip root))]
    (when (and def? node->def)
      (loop [node node, defs []]
        (if-not node
          defs
          (recur (zip/right node)
                 (if (def? node)
                   (conj defs (node->def node))
                   defs)))))))
