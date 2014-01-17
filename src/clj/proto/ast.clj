(ns proto.ast 
  (:require [proto.parser :as p]
            [clojure.zip :as z]))

(declare get-limits* code-zip)

(defn get-limits [buf node-group]
  (-> buf code-zip (get-limits* node-group)))

(defn- tag [node]
  (or (and (map? node) (node :tag)) :default))

(defn- code-zip [root]
  (z/zipper map? :content p/make-node root))

(defn- get-limits*
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

(defn text
  ([tree]
    (-> tree code-zip (text nil)))
  ([loc x]
  (let [[node children :as nxt] (z/next loc)]
    (cond
      (string? node)
        (recur nxt (str x node))
      (not (z/end? nxt))
        (recur nxt x)
      :else x))))

#_(let [code "(defn bla [] (println :bla))";(slurp ".\\src\\proto\\core.clj")
      p    (intern 'proto.parser 'parse)
      zip  (-> code p code-zip)]
  (text-from-ast zip)
)
