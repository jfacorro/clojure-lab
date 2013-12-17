(ns lab.plugin.clojure-lang
  "Clojure language specification."
  (:require [lab.core :as lab]
            [lab.core [plugin :as plugin]
                      [lang :as lang]]
            [lab.model.document :as doc]))

(def grammar [:expr- #{:symbol :keyword :list :string :vector :set :map :regex
                       :number :comment :meta :fn :deref :quote :char}
              :symbol #"[a-zA-Z!$%&*+\-\./<=>?_][a-zA-Z0-9!$%&*+\-\./:<=>?_]*"
              :keyword #"::?#?[\w-_*+\?]+"
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

(def special-forms #{"def" "if" "do" "let" "quote" "var" "'" "fn" "loop" "recur" "throw"
                     "try" "catch" "finally" "monitor-enter" "monitor-exit" "." "new" "set!"})

(def core-vars 
  "Gets all the names for the vars in the clojure.core namespace."
  (->> (the-ns 'clojure.core) ns-interns keys (map str) set))

(def styles
 {:special-form {:color 0xC800C8}
  :var          {:color 0x00FFFF}
  :symbol       {:color 0x64DCB3}
  :delimiter    {:color 0xFFFFFF}
  :accesor      {:color 0x960000}
  :regex        {:color 0xDF6443}
  :keyword      {:color 0x00FF00}
  :namespace    {:color 0x960000}
  :string       {:color 0xE61D43}
  :number       {:color 0xFFFFFF}
  :comment      {:color 0x999988 :bold true}
  :default      {:color 0xFFFFFF}})

(def clojure
  {:name     "Clojure"
   :options  {:main      :expr*
              :root-tag  ::root
              :space :whitespace*
              :make-node lang/make-node}
   :grammar  grammar
   :rank     (partial lang/file-extension? "clj")
   :styles   styles})

(defn init! [app]
  (swap! app assoc-in [:langs :clojure] clojure))

(plugin/defplugin lab.plugin.clojure-lang
  :init! init!)