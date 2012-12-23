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
  "Gets the limits for each string in the tree, ignoring
the limits for the nodes with the tag specified by ignore?."
  ([loc]
    (get-limits loc 0 [] #{:whitespace}))
  ([loc offset limits ignore?]
    (let [[node _ :as nxt] (z/next loc)]
      (cond (string? node)
              (let [new-offset (+ offset (.length node))
                    parent     (-> nxt z/up first)
                    tag        (tag parent)
                    style      (:style (meta parent))
                    limits     (if (ignore? tag)
                                 limits
                                 (conj limits [offset new-offset style]))]
                (recur nxt new-offset limits ignore?))
            (z/end? nxt)
              limits
            :else 
              (recur nxt offset limits ignore?)))))

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