(ns macho.lang.clojure)

;-----------------------------------------------------------
; Special forms and characters collections definitions
;-----------------------------------------------------------
(def special-forms #{"def" "if" "do" "let" "quote" "var" "'" "fn" "loop" "recur" "throw"
                     "try" "catch" "finally" "monitor-enter" "monitor-exit" "." "new" "set!"})

(def symbols (keys (ns-refers *ns*)))

(def delimiters #{"(" ")" "{" "}" "[" "]"})

(def escape-chars-map
  (let [esc-chars "(){}[]*+.?"]
      (zipmap esc-chars
              (map #(str "\\" %) esc-chars))))

;(def blanks (let [bs (wrap (re-escape (conj delimiters "\\s")) "[" "]")
;                 re (]
;                  )

(defn re-escape [s]
  (->> (str s)
       (replace escape-chars-map)
       (reduce str)))

(defn wrap
  "Wraps a string s around the supplied delimiters"
  ([s delim] (wrap s delim delim))
  ([s strt end]
    (apply str (concat strt s end))))

(defn alt-regex [coll]
  (wrap 
    (apply str (interpose "|" (map re-escape coll)))
    "(" ")"))

(def syntax {
  :special-forms {:regex (wrap (alt-regex special-forms) "\\b")
                  :style {:bold true :foreground {:r 0, :g 0, :b 0}}}
  :symbols {:regex (wrap (alt-regex symbols) "\\b")
            :style {:bold true :foreground {:r 0, :g 134, :b 179}}}
  :delimiters {:regex (alt-regex delimiters)
               :style {:bold true :foreground {:r 120, :g 120, :b 120}}}
  :keyword {:regex ":[\\w-!\\?]+"
            :style {:bold true :foreground {:r 153, :g 0, :b 115}}}
  :comment {:regex ";.*[\\n]?"
            :style {:bold true :foreground {:r 153, :g 153, :b 136}}}
  :string-comments {:regex "(?<!\\\\)\".*?(?<!\\\\)\""
           :desc  "Ignore '\\\"' as delimiters."
           :style {:bold true :foreground {:r 223 :g 16, :b 67}}}})
