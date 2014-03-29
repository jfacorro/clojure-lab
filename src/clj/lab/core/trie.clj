(ns lab.core.trie
  (:refer-clojure :exclude [contains?]))

(defn contains?
  "Returns true if the value x exists in the specified trie."
  [trie x]
  (:terminal (get-in trie x) false))

(defn prefix-matches
  "Returns a list of matches with the prefix specified in the trie specified."
  [trie prefix]
  (map :val (filter :val (tree-seq map? vals (get-in trie prefix)))))

(defn add
  "Add a new terminal value."
  [trie x]
  (assoc-in trie x (merge (get-in trie x) {:val x :terminal true})))

(defn trie
  "Builds a trie over the values in the specified seq coll."
  [coll]
  (reduce add {} coll))
