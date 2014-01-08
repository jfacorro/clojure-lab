(ns lab.core.lang
  "Languages are used to determine the structure and syntax of a Document.
They also provide the functions to create and update a parse tree."
  (:require [lab.model.buffer :as buffer]
            [clojure.zip :as z]))

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

(def ^{:dynamic true :private true} *node-group*
  "Determines the value that will be assigned to the nodes 
created during the node generation when creating the parse
tree.")

(defn node-meta
  "If the tag for the node is a symbol
check if its one of the registered symbols."
  [tag content]
  {:style tag :group *node-group*})

(defn make-node [tag content]
  (with-meta {:tag tag :content content}
             (node-meta tag content)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Plain Text Lang

(def plain-text
  (map->Language
    {:name     "Plain text"
     :options  {:main      :expr*
                :root-tag  ::root
                :make-node #'make-node}
     :grammar  [:expr       #"[\s\S]+"]
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
    (buffer/parse-tree (:buffer doc))))

(defn- tag
  "If the node is a map returns its :tag, otherwise the keyword :default."
  [node]
  (or (and (map? node) (node :tag)) :default))

(defn- code-zip
  "Builds a zipper using the root node in the document
under the :parse-tree key."
  [root]
  (z/zipper map? :content make-node root))

(defn- tokens*
  "Gets the limits for each string in the tree, ignoring
the limits for the nodes with the tag specified by ignore?.
If node-group is false all tokens are returned, otherwise
only the tokens from the last tree generation are returned."
  [loc node-group]
  (loop [loc loc, offset 0, limits (transient []), ignore? #{:whitespace}]
    (let [nxt  (z/next loc)
          node (z/node nxt)]
      (cond (string? node)
              (let [length     (.length ^String node)
                    new-offset (+ offset length)
                    parent     (-> nxt z/up z/node)
                    tag        (tag parent)
                    {:keys [style group]} (meta parent)
                    limits     (if (and node-group (or (ignore? tag) (not (= group node-group))))
                                 limits
                                 (conj! limits [offset length style]))]
                (recur nxt new-offset limits ignore?))
            (z/end? nxt)
              (persistent! limits)
            :else 
              (recur nxt offset limits ignore?)))))

(defn tokens
  "Returns the tokens identified incrementally in the parse-tree 
generation that used the group-id identifier provided."
  [root node-group]
  (tokens* (code-zip root) node-group))
