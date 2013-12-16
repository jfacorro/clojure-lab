(ns lab.core.lang
  "Languages are used to determine the structure and syntax of a Document.
They also provide the functions to create and update a parse tree."
  (:require [net.cgrand.parsley :as parsley]))

(def ^{:dynamic true :private true} *node-group*
  "Determines the value that will be assigned to the nodes 
created during the node generation when creating the parse
tree.")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Language 
;;
;; Holds the information for parsing each language
;; using the parsley library.
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
  {:name     "Plain text"
   :options  {:main      :expr*
              :root-tag  ::root
              :make-node #'make-node}
   :grammar  [:expr       #".+"]
   :lang?    (constantly true)
   :styles   {:expr {:color 0xFF}}})

(defn build-buffer [])

(defn parse-tree
  "Parses the incremental buffer of a Document and returns the
parse tree with the modified nodes marked with the value of `group`."
  [doc group]
  (binding [*node-group* group]
    (assoc doc :parse-tree
               (parsley/parse-tree (:buffer doc)))))
