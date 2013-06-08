(ns proto.ast 
  (:require [proto.parser :as p]
            [clojure.pprint :as pp]
            [clojure.zip :as z]))

(defn code-zip [root]
  (z/zipper map? :content p/make-node root))

(defn tag [node]
  (or (and (map? node) (node :tag)) :default))

(defn get-limits
  "Gets the limits for each string in the tree, ignoring
the limits for the nodes with the tag specified by ignore?."
  ([loc node-group]
    (loop [loc loc, offset 0, limits (transient []), ignore? #{:whitespace}]
      (let [[node _ :as nxt] (z/next loc)]
        (cond (string? node)
                (let [new-offset (+ offset (.length node))
                      parent     (-> nxt z/up first)
                      tag        (tag parent)
                      {:keys [style group]}
                                 (meta parent)
                      limits     (if (or (ignore? tag) (not (= group node-group)))
                                   limits
                                   (conj! limits [offset new-offset style]))]
                  (recur nxt new-offset limits ignore?))
              (z/end? nxt)
                (persistent! limits)
              :else 
                (recur nxt offset limits ignore?))))))

(defn print-code-from-ast
  [loc]
    (let [[node children :as nxt] (z/next loc)]
      ;(println nxt)
      (when (string? node)
        (print node))
      (when-not (z/end? nxt)
        (recur nxt))))

#_(let [code "(defn bla [] (println :bla))";(slurp ".\\src\\proto\\core.clj")
        zip  (build-ast code)]
  ;(-> zip z/next z/next pp/pprint)
  ;(println "---------------")
  (get-limits zip))