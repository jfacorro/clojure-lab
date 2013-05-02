(ns lab.ui.core
  (:require [lab.util :as util])
  (:use [lab.ui.protocols :only [Component add children
                                 Abstract impl
                                 Visible visible? hide show
                                 initialize
                                 set-attr
                                 Selected get-selected set-selected]]))

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
  Abstract
  (impl
    ([component]
      (:impl component))
    ([component implementation]
      (assoc component :impl implementation)))
  Visible
  (visible? [this] (-> this impl visible?))
  (hide [this] (-> this impl hide))
  (show [this] (-> this impl show))
  Selected
  (get-selected [this]
    (-> this impl get-selected))
  (set-selected [this selected]
    (-> this impl (set-selected (impl selected)))))

(def component? "Returns true if its single arg is an ui component." :tag)

(def ^:private initialized?
  "Checks if the component is initialized."
  (comp not nil? impl))

(defn- init-content
  "Initialize all content (children) for this component."
  [{content :content :as component}]
  (let [content   (map init content)
        component (assoc component :content [])]
    (reduce add component content)))

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

(defn find-by-tag
  "Finds a child component with the given tag."
  [root tag]
  (let [x (children root)]
    (or (->> x (filter #(= tag (:tag %))) first)
        (->> x (map #(find-by-tag % tag)) (filter identity) first))))

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