(ns macho.lang.clojure)
;-----------------------------------------------------------
; Special forms and characters collections definitions
;-----------------------------------------------------------
(def special-forms #{"def" "if" "do" "let" "quote" "var" "'" "fn" "loop" "recur" "throw"
                     "try" "catch" "finally" "monitor-enter" "monitor-exit" "." "new" "set!"})

;; Get all the vars from the current ns and get them in a set.
(def core-vars 
  "Gets all the names for the vars in the clojure.core namespace."
  (->> (the-ns 'clojure.core) ns-interns keys (map str) set))

(def syntax {
  :special-form {:style {:bold true :foreground {:r 200, :g 0, :b 200}}}
  :var          {:style {:bold true :foreground {:r 0, :g 255, :b 255}}}
  :symbol       {:style {:bold true :foreground {:r 100, :g 220, :b 179}}}
  :delimiter    {:style {:bold true :foreground {:r 255, :g 255, :b 255}}}
  :accesor      {:style {:bold true :foreground {:r 150, :g 0, :b 0}}}
  :regex        {:style {:bold true :foreground {:r 223 :g 100, :b 67}}}
  :keyword      {:style {:bold true :foreground {:r 0, :g 255, :b 0}}}
  :namespace    {:style {:bold true :foreground {:r 150, :g 0, :b 0}}}
  :string       {:style {:bold true :foreground {:r 230 :g 29, :b 67}}}
  :number       {:style {:foreground {:r 255 :g 255, :b 255}}}
  :comment      {:style {:bold true :foreground {:r 153, :g 153, :b 136}}}
  :default      {:style {:foreground {:r 255, :g 255, :b 255}}}})

