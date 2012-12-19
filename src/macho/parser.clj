(ns macho.parser 
  (:use clojure.pprint)
  (:require [net.cgrand.parsley :as p]))
;;------------------------------
(defn make-node [node content]
  {:tag node :content content})
;;------------------------------
;; Giving the grammar a shot
;;---------------------------------
(def options {:main :expr*
              :space :whitespace*
              :make-node make-node})
;;---------------------------------
(def grammar [:expr- #{:symbol :keyword :list :string :vector :set :map :regex
                       :number :comment :meta :fn :deref :quote}
              :symbol #"[a-zA-Z!$%&*+\-\./<=>?_][a-zA-Z0-9!$%&*+\-\./:<=>?_]*"
              :keyword #"::?[\w-_*+]+"
              :whitespace #"[ \t\r\n,]+"
              :list #{["(" :expr* ")"]}
              :vector ["[" :expr* "]"]
              :map ["{" :pair* "}"]
              :set ["#{" :expr* "}"]
              :pair- [:expr :expr]
              :meta ["^" :pair]
              :quote ["'" :expr]
              :regex #"#\"([^\"\\]*|(\\.))*\""
              :string #"(?<!#)\"([^\"\\]*|(\\.))*\""
              :char #"\\(.|newline|space|tab|backspace|formfeed|return|u([0-9a-fA-F]{4}|[0-7]{1,2}|[0-3][0-7]{2}))(?![a-zA-Z0-9!$%&*+\-\./:<=>?_#])"
              :number #"\d+\.?\d*M?"
              :comment #"(#!|;)[^\n\r]*"
              :deref ["@" :expr]
              :fn ["#(" :expr* ")"]])
;;---------------------------------
(def parse (apply p/parser options grammar))
;;---------------------------------
#_(let [code (slurp ".\\src\\macho\\parser.clj")
      ; code "(println #\"regex\" \"string\" #{})"
      tree (time (parse code))]
  (pprint tree))
;;---------------------------------
