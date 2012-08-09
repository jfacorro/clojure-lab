
(ns com.cleasure.lang.clojure.keywords)

(def special-forms #{"def" "if" "do" "let" "quote" "var" "'" "fn" "loop" "recur" "throw"
                     "try" "catch" "finally" "monitor-enter" "monitor-exit" "." "new" "set!"})

(def symbols (keys (ns-refers *ns*)))

(def delimiters #{"(" ")" "{" "}" "[" "]"})

(def blanks (conj delimiters "\\s"))

(defn re-escape [s]
  (-> (str s)
      (.replace "*" "\\*") 
      (.replace "+" "\\+")
      (.replace "." "\\.")
      (.replace "?" "\\?")
      ; (.replace "[" "\\[")
      ; (.replace "]" "\\]")
      (.replace "(" "\\(")
      (.replace ")" "\\)")))

(defn alt-regex [coll]
  (apply str (interpose "|" (map re-escape coll))))

(def syntax {
  :special-forms {:regex (alt-regex special-forms)
                  :style {:bold true :foreground {:r 0, :g 0, :b 0}}}
  :symbols {:regex (alt-regex symbols)
            :style {:bold true :foreground {:r 0, :g 134, :b 179}}}
  :delimiters {:regex (alt-regex delimiters)
               :style {:bold true :foreground {:r 150, :g 150, :b 150}}}
  :keyword {:regex ":\\w+"
            :style {:bold true :foreground {:r 153, :g 0, :b 115}}}
  :comment {:regex ";.*[\\n]?"
            :style {:bold true :foreground {:r 153, :g 153, :b 136}}}
  :string-comments {:regex "(?<!\\\\)\".*?(?<!\\\\)\""
           :desc  "Ignore '\\\"' as delimiters."
           :style {:bold true :foreground {:r 223 :g 16, :b 67}}}})