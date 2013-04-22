(ns poc.ui.core
  (:use [poc.ui.protocols :only [Component Abstract add set-attr create impl]]))

(extend-type clojure.lang.IPersistentMap
  Component ; Extend Clojure maps so that adding children is transparent.
  (add [this child]
    (-> this
      (impl (add (impl this) (impl child)))
      (update-in [:content] conj child)))
  Abstract
  (impl
    ([component]
      (:impl component))
    ([component implementation]
      (assoc component :impl implementation))))

(def component? "Returns true if x is a ui component." :tag)

(declare init)

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

(def ^:private initialized?
  "Checks if the component is initialized."
  impl)


(defn init
  "Initializes a component, creating the implementation for 
  each child and the attributes that have a component as a value."
  [component]
  {:pre [(component? component)]}
  (if (initialized? component) ; do nothing if it's already initiliazed
    component
    (->> (create component)
      (impl component)
      init-content
      set-attrs)))

(defn- build
  "Used by constructor functions to build a component with keys :tag, 
  :attrs and :content.
  
  Possible syntaxes are:
  
    (build :some-tag attr-map content-vector)
    (build :some-tag attr-map)
    (build :some-tag content-vector)
    (build :some-tag :attr-1 val-1 :attr-2 val-2 ... :content content-vector)"
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
(def menu-bar (partial build :menu-bar))
(def menu (partial build :menu))
(def menu-item (partial build :menu-item))
(def text-editor (partial build :text-editor))
(def tabs (partial build :tabs))
(def tab (partial build :tab))
(def scroll (partial build :scroll))

