(ns macho.ast 
  (:require [macho.parser :as p]
            [clojure.pprint :as pp]
            [clojure.zip :as z]))

(defn build-ast [code]
  (let [root (p/parse code)]
    (z/zipper map? :content p/make-node root)))

(defn tag [node]
  (or (and (map? node) (node :tag)) :default))

(defn get-limits
  ([loc]
    (get-limits loc 0 []))
  ([loc offset limits]
    (let [[node _ :as nxt] (z/next loc)]
      (cond (string? node)
              (let [new-offset (+ offset (.length node))
                    tag        (-> nxt z/up first tag)
                    limits     (if (not= tag :whitespace)
                                 (conj limits [offset new-offset tag])
                                 limits)]
                (recur nxt new-offset limits))
            (z/end? nxt)
              limits
            :else 
              (recur nxt offset limits)))))

(defn print-code-from-ast
  [loc]
    (let [[node children :as nxt] (z/next loc)]
      ;(println nxt)
      (when (string? node)
        (print node))
      (when-not (z/end? nxt)
        (recur nxt))))

#_(let [code "(defn bla [] (println :bla))";(slurp ".\\src\\macho\\core.clj")
        zip  (build-ast code)]
  ;(-> zip z/next z/next pp/pprint)
  ;(println "---------------") 
  (get-limits zip))