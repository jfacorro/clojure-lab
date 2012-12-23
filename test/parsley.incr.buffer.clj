(ns test-bufferl
  (:require [macho.parser :as mp]
            [clojure.pprint :as pp]
            [net.cgrand.parsley :as p]))
  
(defn make-node [tag content]
  (println "--- make-node --->" tag)
  (pp/pprint content)
  {:tag tag :content content})

(def parser (apply p/parser (into mp/options {:make-node make-node}) mp/grammar))
(def buf (-> parser p/incremental-buffer (p/edit 0 0 "((a)\n(b)\n(c (:a)))")))

(-> buf p/parse-tree)
(println "-------------------------------")
(-> buf (p/edit 13 1 "") p/parse-tree)
nil
;(pp/pprint (into mp/grammar {:make-node make-node}))

;(meta net.cgrand.parsley/edit)

;(pp/pprint (all-ns))