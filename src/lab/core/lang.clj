(ns lab.core.lang
  "Languages are used to determine the structure and syntax of a Document.
They also provide the functions to create and update a parse tree."
  (:require [lab.model.buffer :as buffer]
            [clojure.zip :as z]))

(def ^{:dynamic true :private true} *node-group*
  "Determines the value that will be assigned to the nodes 
created during the node generation when creating the parse
tree.")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Language 
;;
;; Holds the information for parsing each language.
;; Its fields are self-explanatory except for lang?, which 
;; should hold a predicate function that recieves a doc and
;; returns true if the doc is of that language.

(defrecord Language [name options grammar lang?])

(defn node-meta
  "If the tag for the node is a symbol
check if its one of the registered symbols."
  [tag content]
  {:style tag :group *node-group*})

(defn- make-node [tag content]
  (with-meta {:tag tag :content content}
             (node-meta tag content)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

(def plain-text
  (map->Language
    {:name     "Plain text"
     :options  {:main      :expr*
                :root-tag  ::root
                :make-node #'make-node}
     :grammar  [:expr       #".+"]
     :lang?    (constantly true)
     :styles   {:expr {:color 0xFF}}}))

(defn parse-tree
  "Parses the incremental buffer of a Document and returns the
parse tree with the modified nodes marked with node-group."
  [doc node-group]
  (binding [*node-group* node-group]
    (assoc doc :parse-tree
               (buffer/parse-tree (:buffer doc)))))

(defn- tag
  "If the node is a map returns its :tag, otherwise the keyword :default."
  [node]
  (or (and (map? node) (node :tag)) :default))

(defn- code-zip
  "Builds a zipper using the root node in the document
under the :parse-tree key."
  [doc]
  (let [make-node  (-> doc :lang :options :make-node)
        root       (:parse-tree doc)]
    (z/zipper map? :content make-node root)))

(defn- get-limits*
  "Gets the limits for each string in the tree, ignoring
the limits for the nodes with the tag specified by ignore?."
  ([loc node-group]
    (loop [loc loc, offset 0, limits (transient []), ignore? #{:whitespace}]
      (let [[node _ :as nxt] (z/next loc)]
        (cond (string? node)
                (let [new-offset (+ offset (.length node))
                      parent     (-> nxt z/up first)
                      tag        (tag parent)
                      {:keys [style group]}
                                 (meta parent)
                      limits     (if (or (ignore? tag) (not (= group node-group)))
                                   limits
                                   (conj! limits [offset new-offset style]))]
                  (recur nxt new-offset limits ignore?))
              (z/end? nxt)
                (persistent! limits)
              :else 
                (recur nxt offset limits ignore?))))))

(defn tokens
  "Returns the tokens identified incrementally in the parse-tree 
generation that used the group-id identifier provided."
  [doc node-group]
  (get-limits* (code-zip doc) node-group))
