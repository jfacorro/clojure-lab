(ns macho.lang.clojure)

;-----------------------------------------------------------
; Special forms and characters collections definitions
;-----------------------------------------------------------
(def special-forms #{"def" "if" "do" "let" "quote" "var" "'" "fn" "loop" "recur" "throw"
                     "try" "catch" "finally" "monitor-enter" "monitor-exit" "." "new" "set!"})

;; Get all the vars from the current ns and get them in a set.
(def vars (->> *ns* ns-refers keys (map str) set))

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
  :special-form {:style {:bold true :foreground {:r 0, :g 0, :b 0}}}
  :var        {:style {:bold true :foreground {:r 0, :g 0, :b 200}}}
  :symbol     {:style {:bold true :foreground {:r 0, :g 134, :b 179}}}
  :delimiters {:style {:bold true :foreground {:r 120, :g 120, :b 120}}}
  :accesor    {:style {:bold true :foreground {:r 150, :g 0, :b 0}}}
  :regex      {:style {:bold true :foreground {:r 223 :g 100, :b 67}}}
  :keyword    {:style {:bold true :foreground {:r 153, :g 0, :b 115}}}
  :namespace  {:style {:bold true :foreground {:r 150, :g 0, :b 0}}}
  :string     {:style {:bold true :foreground {:r 223 :g 16, :b 67}}}
  :number     {:style {:foreground {:r 0 :g 0, :b 0}}}
  :comment    {:style {:bold true :foreground {:r 153, :g 153, :b 136}}}})


