(ns lab.core.trie
  (:refer-clojure :exclude [contains?]))

(defn contains? [trie x]
  "Returns true if the value x exists in the specified trie."
  (:terminal (get-in trie x) false))

(defn prefix-matches [trie prefix]
  "Returns a list of matches with the prefix specified in the trie specified."
  (map :val (filter :val (tree-seq map? vals (get-in trie prefix)))))

(defn add [trie x]
  (assoc-in trie x (merge (get-in trie x) {:val x :terminal true})))

(defn trie [coll]
  "Builds a trie over the values in the specified seq coll."
  (reduce add {} coll))
