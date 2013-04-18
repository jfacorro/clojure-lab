(ns poc.ui.protocols)

(defprotocol Component
  (add [this child]))

(defmulti create
  "Creates a component instance based on its :tag."
  :tag)

(defmulti set-attr
  "Sets the attribute value for this component."
  (fn [{tag :tag} k _]
    [tag k]))

(def component? "Returns true if x is a ui component." :tag)

(defn impl 
  "Returns or sets the implementation for this component."
  ([component]
    (:impl component))
  ([component implementation]
    (assoc component :impl implementation)))

(declare init)

(defn- init-content
  "Initialize all content for this component."
  [{content :content :as component}]
  (reduce add component (map init content)))

(defn- attr-reducer
  "Used by set-attrs to reduce all attrs when initializing
  a component."
  [c [k v]]
  (let [v (if (component? v) (init v) v)]
    (set-attr c k v)
    c))

(defn- set-attrs
  "Called when initializing a component. Gets all defined
  attributes and sets their corresponding values."
  [{attrs :attrs :as component}]
  (reduce attr-reducer component attrs))

(def ^:private initialized?
  "Checks if the component is initilized."
  impl)


(defn init
  "Initializes a component, creating the implementation for 
  each child and attributes with a component as a value."
  [component]
  {:pre [(component? component)]}
  (if (initialized? component) ; do nothing if it's already initiliazed
    component
    (->> (create component)
      (impl component)
      init-content
      set-attrs)))

; Extend Clojure maps so that adding is transparent.
(extend-type clojure.lang.PersistentArrayMap
  Component
  (add [this child]
    (add (impl this) (impl child))
    (update-in this [:content] conj child)))

;; Constructor functions
(defn- build
  "Build a map component with keys :tag, :attrs and :content.
  When two args are supplied the second is tested for map?, if 
  it is then it represents the attributes, otherwise it is assumed 
  that it is a vector containing the content."
  ([tag]
    (build tag {} []))
  ([tag x]
    (if (map? x)
      (build tag x [])
      (build tag {} x)))
  ([tag attrs content]
   {:pre [(and (keyword? tag)
               (map? attrs)
               (vector? content))]}
     {:tag tag
     :attrs attrs
     :content content}))
  
(def window (partial build :window))
(def menu-bar (partial build :menu-bar))
(def menu (partial build :menu))
(def menu-item (partial build :menu-item))
(def text-editor (partial build :text-editor))
(def tabs (partial build :tabs))  

