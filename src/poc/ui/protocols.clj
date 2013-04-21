(ns poc.ui.protocols)

(defprotocol Component
  (add [this child] "Add a child to a component. Must return the parent wuth the child added."))

(defmulti create
  "Creates a component instance based on its :tag."
  :tag)

(defmulti set-attr
  "Sets the attribute value for this component and returns the
  modified component."
  (fn [{tag :tag} k _]
    [tag k]))

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

(defn impl
  "Returns or sets the implementation for this component."
  ([component]
    (:impl component))
  ([component implementation]
    (assoc component :impl implementation)))

(def ^:private initialized?
  "Checks if the component is initialized."
  impl)


(defn init
  "Initializes a component, creating the implementation for 
  each child and the attributes that have a component as a value."
  [component]
  ;{:pre [(component? component)]}
  (if (initialized? component) ; do nothing if it's already initiliazed
    component
    (->> (create component)
      (impl component)
      init-content
      set-attrs)))

;; Extend Clojure maps so that adding children is transparent.
(extend-type clojure.lang.IPersistentMap
  Component
  (add [this child]
    (-> this
      (impl (add (impl this) (impl child)))
      (update-in [:content] conj child))))

;; Constructor functions
(defn- build
  "Build a map component with keys :tag, :attrs and :content.
  When two args are supplied the second is tested for map?, if 
  it is then it represents the attributes, otherwise it is assumed 
  that it is a vector containing the content."
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
  
(def window (partial build :window))
(def menu-bar (partial build :menu-bar))
(def menu (partial build :menu))
(def menu-item (partial build :menu-item))
(def text-editor (partial build :text-editor))
(def tabs (partial build :tabs))
(def tab (partial build :tab))
(def scroll (partial build :scroll))

