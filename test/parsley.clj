(ns proto.parser-test
  (:use clojure.pprint)
  (:require [net.cgrand.parsley :as p]))
;;---------------------------------
;; Giving the grammar a shot
;;---------------------------------
(def options {:main :expr*
              :space :space?})

(def grammar [:expr- #{:symbol :keyword :list :string :vector :set :map 
                       :number :comment :meta :fn :deref :quote}
              :symbol #"[a-zA-Z!$%&*+\-\./<=>?_][a-zA-Z0-9!$%&*+\-\./:<=>?_]*"
              :keyword #"::?[\w-_*+]+"              
              :list #{["(" :expr* ")"]}
              :vector ["[" :expr* "]"]
              :map ["{" :pair* "}"]
              :set ["#{" :expr* "}"]
              :pair- [:expr :expr]
              :meta ["^" :pair]
              :quote ["'" :expr]
              :string #"\"([^\"\\]*|(\\.))*\""
              ;:char #"\\(.|newline|space|tab|backspace|formfeed|return|u([0-9a-fA-F]{4}|[0-7]{1,2}|[0-3][0-7]{2}))(?![a-zA-Z0-9!$%&*+\-\./:<=>?_#])"
              :number #"\d+\.?\d*M?"
              :comment #"(#!|;)[^\n]*"
              :deref ["@" :expr]
              :fn ["#(" :expr* ")"]
              :space #"\s+"])

;;---------------------------------
;; Grammar taken from project https://github.com/joodie/clojure-refactoring/
;;---------------------------------
(def options1 {:main :expr*
              :space [#{:whitespace :comment :discard} :*]})

(def grammar1
         [:expr- #{:atom :list :vector :set :string :regex :map :meta :quote :char
                   :syntax-quote :unquote :unquote-splicing :deprecated-meta
                     :deref :var :fn}
          :atom #"[a-zA-Z0-9!$%&*+\-\./:<=>?_][a-zA-Z0-9!$%&*+\-\./:<=>?_#]*"
          :comment #"(#!|;)[^\n]*"
          :whitespace [#"[ \t\n,]+"]
          :list ["(" :expr* ")"]
          :vector ["[" :expr* "]"]
          :set ["#{" :expr* "}"]
          :regex #"#\"([^\"\\]*|(\\.))*\""
          :string #"\"([^\"\\]*|(\\.))*\""
          :pair- [:expr :expr]
          :map ["{" :pair* "}"]
          :discard ["#_" :expr]
          :meta ["^" :pair]
          :quote ["'" :expr]
          :char #"\\(.|newline|space|tab|backspace|formfeed|return|u([0-9a-fA-F]{4}|[0-7]{1,2}|[0-3][0-7]{2}))(?![a-zA-Z0-9!$%&*+\-\./:<=>?_#])"
          :syntax-quote ["`" :expr]
          :tilde- #"~(?!@)"
          :unquote [:tilde :expr]
          :unquote-splicing ["~@" :expr]
          :deprecated-meta ["#^" :pair]
          :deref ["@" :expr]
          :var ["#'" :expr]
          :fn ["#(" :expr* ")"]])
;;---------------------------------
(def p (apply p/parser options grammar))
(def parse (comp :content p))
;;---------------------------------
;; (pprint (parse "(x :bla-* ::bla #{} \"vcdsdcsd\" \\space  [:a :b] (println) 123.2 100)"))
;;---------------------------------
;; (pprint (parse "(ns proto.core) (defn- -is-this.valid&symbol? [] nil)"))
;;---------------------------------
#_(let [code (slurp "../src/proto/main.clj")
      tree (time (parse code))]
  (pprint tree))



