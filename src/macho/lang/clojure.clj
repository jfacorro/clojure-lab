(ns macho.lang.clojure)
;-----------------------------------------------------------
; Special forms and characters collections definitions
;-----------------------------------------------------------
(def special-forms #{"def" "if" "do" "let" "quote" "var" "'" "fn" "loop" "recur" "throw"
                     "try" "catch" "finally" "monitor-enter" "monitor-exit" "." "new" "set!"})

;; Get all the vars from the current ns and get them in a set.
(def core-vars 
  "Gets all the names for the vars in the clojure.core namespace."
  (->> (the-ns 'clojure.core) ns-refers keys (map str) set))

(def syntax {
  :special-form {:style {:bold true :foreground {:r 0, :g 0, :b 0}}}
  :var          {:style {:bold true :foreground {:r 0, :g 0, :b 200}}}
  :symbol       {:style {:bold true :foreground {:r 0, :g 134, :b 179}}}
  :delimiters   {:style {:bold true :foreground {:r 255, :g 255, :b 255}}}
  :accesor      {:style {:bold true :foreground {:r 150, :g 0, :b 0}}}
  :regex        {:style {:bold true :foreground {:r 223 :g 100, :b 67}}}
  :keyword      {:style {:bold true :foreground {:r 153, :g 0, :b 115}}}
  :namespace    {:style {:bold true :foreground {:r 150, :g 0, :b 0}}}
  :string       {:style {:bold true :foreground {:r 223 :g 16, :b 67}}}
  :number       {:style {:foreground {:r 0 :g 0, :b 0}}}
  :comment      {:style {:bold true :foreground {:r 153, :g 153, :b 136}}}})


