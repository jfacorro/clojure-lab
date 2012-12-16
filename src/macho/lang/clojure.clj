(ns macho.lang.clojure)

;-----------------------------------------------------------
; Special forms and characters collections definitions
;-----------------------------------------------------------
(def special-forms #{"def" "if" "do" "let" "quote" "var" "'" "fn" "loop" "recur" "throw"
                     "try" "catch" "finally" "monitor-enter" "monitor-exit" "." "new" "set!"})

(def symbols (keys (ns-refers *ns*)))

(def delimiters #{"(" ")" "{" "}" "[" "]" "#" ","})
(def delimiters-plus-space (conj delimiters " "))

(def escape-chars-map
  (let [esc-chars "(){}[]*+.?"]
      (zipmap esc-chars
              (map #(str \\ %) esc-chars))))

(defn re-escape [s] 
  (->> (str s)
       (replace escape-chars-map)
       (reduce str)))

(def blanks-behind (str "(?<=[" (apply str (map re-escape delimiters-plus-space)) "]|^)"))
(def blanks-ahead (str "(?=[" (apply str (map re-escape delimiters-plus-space)) "]|$)"))

(defn wrap
  "Wraps a string s around the supplied delimiters" 
  ([s delim] (wrap s delim delim))
  ([s strt end]
    (apply str (concat strt s end))))

(defn alt-regex [coll]
  (wrap (interpose "|" (map re-escape coll)) "(" ")"))

(defn class-regex [coll]
  (wrap (interpose "" (map re-escape coll)) "[" "]"))

(defn wrap-blanks [s]
  (wrap s blanks-behind blanks-ahead))

(def syntax {
  :special-forms {:regex (wrap-blanks (alt-regex special-forms))
                  :style {:bold true :foreground {:r 0, :g 0, :b 0}}}
  :symbols {:regex (wrap-blanks (alt-regex symbols))
            :style {:bold true :foreground {:r 0, :g 134, :b 179}}}
  :delimiters {:regex (class-regex delimiters)
               :style {:bold true :foreground {:r 120, :g 120, :b 120}}}
  :accesor {:regex "(?<=\\()\\.\\w+"
            :style {:bold true :foreground {:r 150, :g 0, :b 0}}}
  :keyword {:regex (wrap-blanks "\\^?:[\\w-!\\?]+")
            :style {:bold true :foreground {:r 153, :g 0, :b 115}}}
  :namespace {:regex (wrap-blanks "(?s)(?:\\w+\\.|(?<=\\.)\\w+)+(?:/[\\w-_]+)?")
              :style {:bold true :foreground {:r 150, :g 0, :b 0}}}
  :string {:regex "(?s)(?<!\\\\)\".*?(?<!\\\\)\""
           :desc  "Ignore '\\\"' as delimiters."
           :style {:bold true :foreground {:r 223 :g 16, :b 67}}}
  :comment {:regex ";.*\\n"
            :style {:bold true :foreground {:r 153, :g 153, :b 136}}}})


