(ns lab.ui.core
  (:refer-clojure :exclude [find])
  (:require [lab.util :as util]
            [lab.ui.select :as sel]
            [lab.ui.hierarchy :as h]
            [lab.ui.protocols :as p]))

(declare init initialized?)

;; Convenience macros for multimethod implementations

(defmacro definitializations
  "Generates all the multimethod implementations
  for each of the entries in the map destrcutured
  from its args.
  
  :component-name ClassName or init-fn"
  [& {:as m}]
  `(do
      ;(remove-all-methods initialize) ; this is useful for developing but messes up the ability to break implementations into namespaces
    ~@(for [[k c] m]
      (if (-> c resolve class?)
        `(defmethod p/initialize ~k [c#]
          (new ~c))
        `(defmethod p/initialize ~k [x#]
          (~c x#))))))

(defmacro defattributes
  "Convenience macro to define attribute setters for each
  component type.
  The method implemented always returns the first argument which 
  is supposed to be the component itself.

    *attrs-declaration
  
  Where each attrs-declaration is:
 
    component-keyword *attr-declaration
    
  And each attr-declaration is:

   (attr-name [c k v] & body)"
  [& body]
  (let [comps (->> body 
                (partition-by keyword?) 
                (partition 2) 
                (map #(apply concat %)))
        f     (fn [tag & mthds]
                (for [[attr [c _ _ :as args] & body] mthds]
                  `(defmethod p/set-attr [~tag ~attr]
                    ~args 
                    ~@body 
                    ~c)))]
    `(do ~@(mapcat (partial apply f) comps))))


;; Every abstract component is represented by a Clojure map.

(extend-type clojure.lang.IPersistentMap
  p/Component ; Extend Clojure maps so that adding children is transparent.
  (children [this]
    (:content this))
  (add [this child]
    (let [this  (init this)
          child (init child)]
      (-> this
        (p/impl (p/add (p/impl this) (p/impl child)))
        (update-in [:content] conj child))))
  (remove [this child]
    (let [i (.indexOf ^java.util.List (p/children this) child)]
      (-> this
        (p/impl (p/remove (p/impl this) (p/impl child)))
        (update-in [:content] util/remove-at i))))
  (add-binding [this ks f]
    (let [this (init this)
          i    (p/impl this)]
      (p/impl this (p/add-binding i ks f))))
  (remove-binding [this ks]
    (let [i (p/impl this)]
      (p/impl this (p/remove-binding i ks))))

  p/Abstract
  (impl
    ([component]
      (-> component meta :impl))
    ([component implementation]
      (vary-meta component assoc :impl implementation)))

  p/Visible
  (visible? [this]
    (-> this p/impl p/visible?))
  (hide [this]
    (-> this p/impl p/hide))
  (show [this]
    (-> this p/impl p/show))

  p/Selected
  (selected
    ([this]
      (-> this p/impl p/selected))
    ([this selected]
      (-> this p/impl (p/selected (p/impl selected))))))

(defn component? 
  "Returns true if its arg is a component."
  [x]
  (or (and (map? x)
           (x :tag))
      (and (vector? x)
           (isa? h/hierarchy (first x) :component))))

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
  (comp not nil? p/impl))

(defn- init-content
  "Initialize all content (children) for this component."
  [{content :content :as component}]
  (let [content   (map init content)
        component (assoc component :content [])]
    (reduce p/add component content)))

(def genid
  "Generates a unique id string."
  #(name (gensym)))

(defn set-attr
  "Uses the set-attr multimethod to set the attribute value 
  for the implementation and updates the abstract component
  as well."
  [c k v]
  (let [c (p/set-attr c k v)]
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
      (let [ctrl       (p/initialize component)
            component  (-> component
                         (p/impl ctrl)
                         set-attrs
                         init-content)
            ctrl       (p/abstract ctrl component)]
        component))))

(defn find
  "Returns the first component found."
  [root selector]
  (when-let [path (sel/select root selector)]
    (get-in root path)))

(defn update
  "Updates all the components that match the selector expression
  using (update-in root path-to-component f args)."
  [root selector f & args]
  (reduce (fn [x path]
            (if (empty? path)
              (apply f x args)
              (apply update-in x path f args)))
          root
          (sel/select-all root selector)))

(defn update!
  "Same as update but assumes root is an atom." 
  [root selector f & args]
  {:pre [(instance? clojure.lang.Atom root)]}
  (apply swap! root update selector f args))

; Event

(defn event-handler
  "Builds a function that swap!s the x using
f, which should take a value and an event."
  ([f]
    (fn [x evt]
      (assert (instance? clojure.lang.IRef x) (str "x should be a reference. f: " f " - event: " (class evt)))
      (swap! x f evt)))
  ([f x]
    (partial (event-handler f) x)))
