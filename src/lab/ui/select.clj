(ns lab.ui.select
  (:refer-clojure :exclude [compile])
  (:use [lab.ui.protocols :only [Component add children]]))

;; Selector functions

(defn tag=
  "Returns a predicate that returns true if the
  component arg received has the specified tag."
  [tag]
  (with-meta #(= tag (:tag %)) {:tag tag}))

(defn id=
  [id]
  (with-meta #(= id (-> % :attrs :-id)) {:id id}))

(defn attr= [attr v]
  (with-meta
    #(-> % :attrs attr (= v))
    {:attr attr :value v}))

(defn attr? [attr]
  (with-meta
    #(-> % :attrs attr)
    {:attr attr}))

;; Parsing functions

(defn- id?
  "Returns true if the string begins with a
  hash (#) sign which indicated its an id selector."
  [s]
  (when-let [[x & _] (and (keyword? s) (name s))]
    (= x \#)))

(defn- tag?
  "Returns true if the string doesn't begin with a
  hash (#) and only has a single."
  [s]
  (when-let [[x & _] (and (keyword? s) (name s))]
    (not= x \#)))

(defn- parse
  "Takes a selector (keyword) and parses it identifying
  its type and value, returning it in a vector.
  For example:
    :#main [:id \"main\"]
    :label [:tag :label]"
  [s]
  (cond (id? s)
          [:id (->> s name rest (apply str))]
        (tag? s)
          [:tag s]
        (fn? s)
          [:fn s]))

;; Path finding

(defn- find-path
  "Returns the path to the child component that satisfies (pred component)."
  [pred component]
  (if (pred component)
    []
    (if-let [path (->> component
                    children
                    (map-indexed #(vector %1 (find-path pred %2)))
                    (filter second)
                    first)]
      (-> [:content] (concat path) flatten vec))))

(defn- compile
  "Takes a selector and returns a single arg predicate."
  [selector]
  (if (vector? selector)
    (let [predicates (map compile selector)]
      (fn [x]
        (reduce #(and % (%2 x)) true predicates)))
    (let [[t v] (parse selector)]
      (condp = t
        :id  (id= v)
        :tag (tag= v)
        :fn  v))))

(defn- chain
  "Reducer function used for chaining selectors."
  [[path root] pred]
  (let [path' (find-path pred root)
        path  (when path' (concat path path'))
        root  (get-in root path)]
    [path root]))

(defn select
  "Takes a selection expression and returns the path for the
  matching components from root."
  [root selector]
  (let [selector   (if (vector? selector) selector [selector])
        predicates (map compile selector)
        [path _]   (reduce chain [nil root] predicates)]
    path))