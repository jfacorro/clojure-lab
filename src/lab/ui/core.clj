(ns lab.ui.core
  (:require [lab.util :as util]
            [lab.ui.protocols :as p])
  (:use [lab.ui.protocols :only [Component add children
                                 Abstract impl
                                 Visible visible? hide show
                                 Selected get-selected set-selected
                                 initialize]]))

(declare init initialized?)

(extend-type clojure.lang.IPersistentMap
  Component ; Extend Clojure maps so that adding children is transparent.
  (children [this]
    (:content this))
  (add [this child]
    (let [this  (init this)
          child (init child)]
      (-> this
        (impl (add (impl this) (impl child)))
        (update-in [:content] conj child))))
  (p/remove [this child]
    (let [i (.indexOf (children this) child)]
      (-> this
        (impl (p/remove (impl this) (impl child)))
        (update-in [:content] util/remove-at i))))
  Abstract
  (impl
    ([component]
      (:impl component))
    ([component implementation]
      (assoc component :impl implementation)))
  Visible
  (visible? [this]
    (-> this impl visible?))
  (hide [this]
    (-> this impl hide))
  (show [this]
    (-> this impl show))
  Selected
  (get-selected [this]
    (-> this impl get-selected))
  (set-selected [this selected]
    (-> this impl (set-selected (impl selected)))))

(def component? "Returns true if its arg is a component." :tag)

(def ^:private initialized?
  "Checks if the component is initialized."
  (comp not nil? impl))

(defn- init-content
  "Initialize all content (children) for this component."
  [{content :content :as component}]
  (let [content   (map init content)
        component (assoc component :content [])]
    (reduce add component content)))

(defn set-attr
  [c k v]
  (let [c (or (and (namespace k) c)
              (p/set-attr c k v))]
    (assoc-in c [:attrs k] v)))

(defn- attr-reducer
  "Used by set-attrs to reduce all attrs when initializing
  a component."
  [c [k v]]
  (let [v (if (component? v) (init v) v)]
    (set-attr c k v)))

(defn- set-attrs
  "Called when initializing a component. Gets all defined
  attributes and sets their corresponding values."
  [{attrs :attrs :as component}]
  (reduce attr-reducer component attrs))

(defn init
  "Initializes a component, creating the implementation for 
  each child and the attributes that have a component as a value."
  [component]
  {:pre [(component? component)]}
  (if (initialized? component) ; do nothing if it's already initiliazed
    component
    (->> (initialize component)
      (impl component)
      set-attrs
      init-content)))

(defn find-path-by
  "Returns the path to the child component that satisfies (= val (f com))."
  [f component value]
  (let [ch (children component)
        y (->> ch (filter #(= value (f %))) first)]
    (if y
        [:content (.indexOf ch y)]
        (when-let [res (->> ch (map-indexed #(vector %1 (find-path-by f %2 value)))
                               (filter second)
                               first)]
          (-> [:content] (concat res) flatten vec)))))

(def by-tag :tag)
(def by-id #(-> % :attrs :ui/id))

(def find-path-by-tag
  "Finds a child component with the given tag."
  (partial find-path-by by-tag))
  
(def find-path-by-id
  "Finds a child component with the given id."
  (partial find-path-by by-id))

(defn find-by
  "Finds a child component with the given value in the
  specified attribute."
  [f component value]
  (when-let [path (find-path-by f component value)]
    (get-in component path)))

(def find-by-tag
  "Finds a child component with the given tag."
  (partial find-by by-tag))
  
(def find-by-id
  "Finds a child component with the given id."
  (partial find-by by-id))

(defn update-by-id
  "Updates the the component with the supplied id by calling
  the function f with it as its sole argument."
  [root id f]
  (update-in root (find-path-by by-id root id) f))

(defn- build
  "Used by constructor functions to build a component with keys :tag, 
  :attrs and :content.
  
  Usage:  
    (build tag attr-map content-vector)
    (build tag attr-map)
    (build tag content-vector)
    (build tag key val & kvs)"
  ([tag]
    (build tag {} []))
  ([tag & [x y & _ :as xs]]
   {:pre [(keyword? tag)]}
   (cond (map? x)
           {:tag tag :attrs x :content (or y [])}
         (vector? x)
           {:tag tag :attrs {} :content x}
         (keyword? x)
           (let [attrs   (apply hash-map xs)
                 content (:content attrs)]
             {:tag tag :attrs (dissoc attrs :content) :content (or content [])}))))

;;-----------------------
;; Constructor functions
;;-----------------------
(def window (partial build :window))
(def panel (partial build :panel))
(def split (partial build :split))

(def menu-bar (partial build :menu-bar))
(def menu (partial build :menu))
(def menu-item (partial build :menu-item))

(def text-editor (partial build :text-editor))
(def font (partial build :font))

(def tabs (partial build :tabs))
(def tab (partial build :tab))

(def tree (partial build :tree))
(def tree-node (partial build :tree-node))

(def label (partial build :label))
(def button (partial build :button))