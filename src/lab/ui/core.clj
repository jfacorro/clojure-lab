(ns lab.ui.core
  (:refer-clojure :exclude [find])
  (:require [lab.util :as util]
            [lab.ui.select :as sel]
            [lab.ui.protocols :as p])
  (:use [lab.ui.protocols :only [Component add children
                                 Abstract impl
                                 Implementation abstract
                                 Visible visible? hide show
                                 Selected get-selected set-selected
                                 initialize]]))

(declare init initialized?)

;; Every abstract component is represented by a Clojure map.

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
      (-> component meta :impl))
    ([component implementation]
      (vary-meta component assoc :impl implementation)))

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
  (let [content   (if (sequential? content) content [content])
        content   (map init content)
        component (assoc component :content [])]
    (reduce add component content)))

(defn- abstract-attr?
  [k]
  (-> k name first #{\-}))

(def gen-id
  "Generates a unique id string."
  #(name (gensym)))

(defn set-attr
  "Uses the set-attr multimethod to set the attribute value 
  for the implementation and updates the abstract component
  as well."
  [c k v]
  (let [c (if (abstract-attr? k)
            c
            (p/set-attr c k v))]
    (assoc-in c [:attrs k] v)))

(defn get-attr
  "Returns the attribute k from the component."
  [c k]
  (-> c :attrs k))

(defn- set-attrs
  "Called when initializing a component. Gets all defined
  attributes and sets their corresponding values."
  [{attrs :attrs :as component}]
  (let [f (fn [c [k v]]
            (set-attr c k (if (component? v) (init v) v)))]
    (reduce f component attrs)))

(defn init
  "Initializes a component, creating the implementation for 
  each child and the attributes that have a component as a value."
  [component]
  {:pre [(component? component)]}
  (if (initialized? component) ; do nothing if it's already initiliazed
    component
    (let [ctrl       (initialize component)
          component  (-> component
                       (impl ctrl)
                       set-attrs
                       init-content)
          ctrl       (abstract ctrl component)]
      (impl component ctrl))))

(defn find
  [root selector]
  (when-let [path (sel/select root selector)]
    (get-in root path)))

(defn update
  "Updates the component that matches the selector expression
  using:
    (update-in root (path-to-selector) f args)."
  [root selector f & args]
  (if-let [path (sel/select root selector)]
    (apply update-in root path f args)
    root))

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

(def tabs (partial build :tabs))
(def tab (partial build :tab))

(def tree (partial build :tree))
(def tree-node (partial build :tree-node))

(def label (partial build :label))
(def button (partial build :button))
