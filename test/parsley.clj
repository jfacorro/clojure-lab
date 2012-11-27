(ns parsley
  (:use clojure.pprint)
  (:require [net.cgrand.parsley :as p]))

(def options {:main :expr*
              :space :space?})

(def grammar [:symbol #{#"\w+"}
              :keyword #{#":[\w-_*+]+"}
              :expr #{:expr-rep}
              :expr-rep- #{["(" :expr* ")"]}              
              ;:number #{#"\d+M?"}              
              :space #{#"\s+"}])

(def p (apply p/parser options grammar))
(pprint (p "(x (x) :bla-*) (((println :bla))) 0123"))

; running on malformed input with garbage
; (pprint (p "a(zldxn(dez)"))
