(ns lab.ui.select
  (:use [lab.ui.protocols :only [Component add children]]))

;; Selector function generators

(defn- tag-selector
  "Returns a predicate that returns true if the
  component arg received has the specified tag."
  [tag]
  #(= tag (:tag %)))

(defn- id-selector 
  [id]
  #(= id (-> % :attrs :-id)))

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

(defn- compile-selector
  "Takes a selector and returns a single arg function 
  that will build the path to use with functions like 
  get-in and update-in."
  [selector]
  (if (vector? selector)
    (let [selectors (map compile-selector selector)]
      (fn [x]
        (reduce (fn [acc f] (and acc (find-path f x)))
                true
                selectors)))
    (let [[t v] (parse selector)]
      (condp = t
        :id  (partial find-path (id-selector v))
        :tag (partial find-path (tag-selector v))
        :fn  (partial find-path v)))))

(defn- chain
  "Reducer function used for chaining selectors."
  [[path root] f]
  (let [path' (f root)
        path  (when path' (concat path path'))
        root  (get-in root path)]
    [path root]))

(defn select
  "Takes a selection expression and returns the path for the
  matching components from root."
  [root selector]
  (if-let [selectors (and (vector? selector) (map compile-selector selector))]
    (->> selectors (reduce chain [nil root]) first)
    ((compile-selector selector) root)))
