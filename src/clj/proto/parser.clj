(ns proto.parser 
  (:use clojure.pprint)
  (:require [net.cgrand.parsley :as p]
            [proto.lang.clojure :as lang]))
;;------------------------------
(defrecord Language [^String name 
                     ^clojure.lang.IPersistentMap grammar 
                     ^clojure.lang.IPersistentCollection exts])
;;------------------------------
(def ^{:dynamic true :private true} *node-group*)
;;------------------------------
(defn check-symbol [name]
  (cond (lang/special-forms name)
          :special-form
        (lang/core-vars name)
          :var
        :else
          :symbol))
;;------------------------------
(defn node-meta 
  "If the tag for the node is a symbol
check if its one of the registered symbols."
  [tag content]
  (let [style (if (= tag :symbol)
                (-> content first check-symbol)
                tag)]
    {:style style :group *node-group*}))
;;------------------------------
(defn make-node [tag content]
  (with-meta {:tag tag :content content}
             (node-meta tag content)))
;;------------------------------
;; Giving the grammar a shot
;;---------------------------------
(def options {:main :expr*
              :root-tag ::root
              :space :whitespace*
              :make-node #'make-node})
;;---------------------------------
(def grammar [:expr- #{:symbol :keyword :list :string :vector :set :map :regex
                       :number :comment :meta :fn :deref :quote :char}
              :symbol #"[a-zA-Z!$%&*+\-\./<=>?_][a-zA-Z0-9!$%&*+\-\./:<=>?_]*"
              :keyword #"::?[\w-_\*\+\?#/>]+"
              :whitespace #"[ \t\r\n,]+"
              :list [#"(?<!\\)\(" :expr* #"(?<!\\)\)"]
              :vector ["[" :expr* "]"]
              :map ["{" :pair* "}"]
              :set ["#{" :expr* "}"]
              :pair- [:expr :expr]
              :meta ["^" :pair]
              :char #"\\."
              :quote ["'" :expr]
              :regex #"#\"([^\"\\]*|(\\.))*\""
              :string #"(?s)(?<!#)\".*?(?<!\\)\""
              :char #"\\(.|newline|space|tab|backspace|formfeed|return|u([0-9a-fA-F]{4}|[0-7]{1,2}|[0-3][0-7]{2}))(?![a-zA-Z0-9!$%&*+\-\./:<=>?_#])"
              :number #"\d+\.?\d*M?"
              :comment #"(#!|;).*[^\n\r]*"
              :deref ["@" :expr]
              :fn ["#(" :expr* ")"]])
;;---------------------------------
(def ^:private parse (apply p/parser options grammar))
;;---------------------------------
(defn edit [buf offset len text]
  (p/edit buf offset len text))
;;---------------------------------
(defn make-buffer [src]
  (-> parse p/incremental-buffer (edit 0 0 src)))
;;---------------------------------
(defn parse-tree
  "Parse the incremental buffer and tag the new nodes with the
  node-group provided."
  [buf node-group]
  (binding [*node-group* node-group]
    (p/parse-tree buf)))
;;---------------------------------
