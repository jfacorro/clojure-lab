(ns lab.ui.core
  (:refer-clojure :exclude [find])
  (:require [lab.util :as util]
            [lab.ui.select :as sel]
            [lab.ui.protocols :as p :reload true])
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

(def components #{; containers
                  :window :panel :split :scroll
                  ; menu
                  :menu-bar :menu :menu-item :menu-separator 
                  ; text
                  :text-editor
                  ; tabs
                  :tabs :tab
                  ; tree
                  :tree :tree-node
                  ; misc
                  :button :label})

(defn component? 
  "Returns true if its arg is a component."
  [x]
  (or (and (map? x)
           (x :tag)) 
      (and (vector? x)
           (-> x first components))))

(defn hiccup->map
  "Used to convert huiccup syntax declarations to map components.
  
  x: [tag-keyword attrs-map? children*]"
  [x]
    (if (vector? x)
      (let [[tag & [attrs & ch :as children]] x]
        {:tag     tag 
         :attrs   (if-not (component? attrs) attrs {})
         :content (mapv hiccup->map (if (component? attrs) children ch))})
      x))


(def ^:private initialized?
  "Checks if the component is initialized."
  (comp not nil? impl))

(defn- init-content
  "Initialize all content (children) for this component."
  [{content :content :as component}]
  (let [content   (map init content)
        component (assoc component :content [])]
    (reduce add component content)))

(defn- abstract-attr?
  [k]
  (-> k name first #{\-}))

(def genid
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
  {:post [(component? component)]}
  (let [component (hiccup->map component)]
    (if (initialized? component) ; do nothing if it's already initiliazed
      component
      (let [ctrl       (initialize component)
            component  (-> component
                         (impl ctrl)
                         set-attrs
                         init-content)
            ctrl       (abstract ctrl component)]
        (impl component ctrl)))))

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
    (if (empty? path)
      (apply f root args)
      (apply update-in root path f args))
    root))

(defn add-binding
  "Adds a key binding to this component."
  [c ks f]
  (let [c (init c)
        i (impl c)]
    (impl c (p/add-binding i ks f))))

(defn remove-binding
  "Adds a key binding to this component."
  [c ks]
  (let [i (impl c)]
    (impl c (p/remove-binding i ks))))
