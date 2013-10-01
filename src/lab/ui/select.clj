(ns lab.ui.select
  (:refer-clojure :exclude [compile])
  (:require [clojure.zip :as zip])
  (:use [lab.ui.protocols :only [Component add children]]))

;; Selector functions

(defn tag=
  "Returns a predicate that indicates whether its 
  argument received has the specified tag name."
  [tag]
  (with-meta #(= tag (:tag %)) {:tag tag}))

(defn id=
  "Returns a predicate that indicates wheter its
  argument has the same id as the provided."
  [id]
  (with-meta #(= id (-> % :attrs :-id)) {:id id}))

(defn attr= [attr v]
  "Returns a predicate that indicates whether its
  argument has the value provided in the attribute specified."
  (with-meta
    #(-> % :attrs attr (= v))
    {:attr attr :value v}))

(defn attr? [attr]
  "Returns a predicate that indicates whether its
  argument has a truthy value in the attribute specified."
  (with-meta
    #(-> % :attrs attr)
    {:attr attr}))

;; Parsing functions

(defn- literal-selector?
  "Indicates whether x is a literal selector (keyword or string), 
  as opposed to a function selector, by returning (name x) if it 
  is or false otherwise"
  [x]
  (and (or (string? x) (keyword? x)) 
       (name x)))

(defn- id?
  "Returns true if the string begins with a
  hash (#) sign which indicates its an id selector."
  [s]
  (when-let [[x & _] (literal-selector? s)]
    (= x \#)))

(defn- tag?
  "Returns true if the string doesn't begin with a
  hash (#) and only has a single."
  [s]
  (when-let [[x & _] (literal-selector? s)]
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

(defn- compile
  "Takes a selector and returns a single arg predicate."
  [selector]
  (if (sequential? selector)
    ; Conjunction predicate
    (let [predicates (map compile selector)]
      (fn [x]
        (reduce #(and % (%2 x)) true predicates)))
    ; Simple predicate
    (let [[t v] (parse selector)]
      (condp = t
        :id  (id= v)
        :tag (tag= v)
        :fn  v))))

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

(defn- chain
  "Reducer function used for applying selectors (pred) in a chain.
  Uses find-path on root to get the next level in the of the path."
  [[path root] pred]
  (let [path' (find-path pred root)
        path  (when path' (concat path path'))
        root  (get-in root path)]
    [path root]))

(defn select
  "Takes a selection expression and returns the path for the
  first matching component from root, which must be a component.
  
  The format of the selector mimics enlive selectors, the following
  is the syntax for each type of selector:
  
  Id          :#value-id
  Tag         :tag-name
  Unary pred  (fn [c] true)    

  TODO: returns a list of all matching components."
  [root selector]
  (when selector
    (let [selector   (if (sequential? selector) selector [selector])
          predicates (map compile selector)
          [path _]   (if (-> predicates count pos?)
                       (reduce chain [nil root] predicates)
                       [[] nil])]
      path)))

(defn merge-result [src [nodes _]]
  (reduce (fn [[result index] node]
            (if (and node (-> node zip/node index not))
              [(conj result node) (conj index (zip/node node))]
              [result index]))
          src
          nodes))

(defn- find-all-paths [node orig-preds [p & ps :as preds]]
  (let [match?     (-> node zip/node p)
        children?  (-> node zip/children seq)
        rights?    (-> node zip/rights seq)
        m1         (when (and match? (not ps))
                     [[node] #{(zip/node node)}])
        m2         (when (and match? ps children?)
                     (find-all-paths (zip/down node) orig-preds ps))
        m3         (when children?
                     (find-all-paths (zip/down node) orig-preds orig-preds))
        [m4 m5]    (when rights?
                     [(find-all-paths (zip/right node) orig-preds preds)
                      (find-all-paths (zip/right node) orig-preds orig-preds)])
        result     (reduce merge-result [[] #{}] [m1 m2 m3 m4 m5])]
    result))

(defn select-all
  [root selector]
  (when selector
    (let [selector   (if (sequential? selector) selector [selector])
          predicates (map compile selector)
          root       (zip/zipper map? :content identity root)
          [nodes _]  (if (-> predicates count pos?)
                       (find-all-paths root predicates predicates)
                       [])]
      (mapv zip/node nodes))))
  