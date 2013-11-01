(ns lab.ui.core
  "Provides the API to create and manipulate UI component 
abstractions as Clojure records. Components can be declared as 
maps or in a hiccup format. Existing tags are defined in an ad-hoc 
hierarchy which can be extended as needed.

Implementation of components is based on the definitialize and defattribute
multi-methods. The former should return an instance of the underlying UI object,
while the latter is used for setting its attributes' value defined in the 
abstract specification (or explicitly through the use of set-attr).

Example: the following code creates a 300x400 window with a \"Hello!\" button
         and shows it on the screen.

  (-> [:window {:size [300 400]} [:button {:text \"Hello!\"}]]
    init
    show)"
  (:refer-clojure :exclude [find remove])
  (:require [lab.util :as util]
            [lab.ui.protocols :as p]
            [lab.ui.select :as sel]
            [lab.ui.hierarchy :as h]))

(declare init initialized? set-attr)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
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
  "Convenience macro to define attribute setters for each component type. 

The method implemented returns the first argument (which is the component 
itself), UNLESS the ^:modify metadata flag is true for the argument vector, 
in which case the value from the body evaluation is returned.

  *attrs-declaration
  
Where each attrs-declaration is:

  component-keyword *attr-declaration
    
And each attr-declaration is:

  (attr-name [c attr v] & body)"
  [& body]
  (let [comps (->> body 
                (partition-by keyword?) 
                (partition 2) 
                (map #(apply concat %)))
        f     (fn [tag & mthds]
                (for [[attr [c _ _ :as args] & body] mthds]
                  (let [x (gensym)]
                    `(defmethod p/set-attr [~tag ~attr]
                      ~args
                      (let [~x (do ~@body)]
                        ~(if (-> args meta :modify) x c))))))]
    `(do ~@(mapcat (partial apply f) comps))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Abstract UI Component record

(defrecord UIComponent [tag attrs content]
  p/Abstract
  (impl [component]
    (-> component meta :impl))
  (impl [component implementation]
    (vary-meta component assoc :impl implementation))

  p/Selected
  (selected [this]
      (p/selected (p/impl this)))
  (selected [this selected]
      (p/selected (p/impl this) (p/impl selected))))

;; Have to use this since remove is part of the java.util.Map interface.
(extend-type UIComponent
  p/Component 
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
      (p/impl this (p/remove-binding i ks)))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Expose Protocol Functions

(defn children [c] (p/children c))
(defn add [c child] (p/add c child))
(defn remove [c child] (p/remove c child))

(defn selected [c] (p/selected c))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Private supporting functions

(defn- component?
  "Returns true if its arg is a component."
  [x]
  (or (instance? UIComponent x)
      (and (vector? x)
           (isa? h/hierarchy (first x) :component))))

(defn- hiccup->component
  "Used to convert huiccup syntax declarations to map components.
x should be a vector with the content [tag-keyword attrs-map? children*]"
  [x]
    (if (vector? x)
      (let [[tag & [attrs & ch :as children]] x]
        (->UIComponent
          tag 
          (if-not (component? attrs) attrs {})
          (mapv hiccup->component (if (component? attrs) children ch))))
      (update-in x [:content] (partial mapv hiccup->component))))

(def ^:private initialized?
  "Checks if the component is initialized."
  (comp not nil? p/impl))

(defn- init-content
  "Initialize all content (children) for this component."
  [{content :content :as component}]
  (let [content   (map init content)
        component (assoc component :content [])]
    (reduce p/add component content)))

(defn- set-attrs
  "Called when initializing a component. Gets all defined
attributes and sets their corresponding values."
  [{attrs :attrs :as component}]
  (let [f (fn [c [k v]]
            (set-attr c k (if (component? v) (init v) v)))]
    (reduce f component attrs)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initialization and Attributes Access

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

(defn init
  "Initializes a component, creating the implementation for 
each child and the attributes that have a component as a value."
  [c]
  {:post [(component? c)]}
  (let [c (hiccup->component c)]
    (if (initialized? c) ; do nothing if it's already initiliazed
      c
      (let [ctrl  (p/initialize c)
            c     (-> c
                    (p/impl ctrl)
                    set-attrs
                    init-content)
            ctrl  (p/abstract ctrl c)]
        c))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Finding and Updating

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Event Handling

(defn event-handler
  "Builds a function that swap!s the x using
f, which should take a value and an event."
  ([f]
    (fn [x evt]
      (assert (instance? clojure.lang.IRef x) (str "x should be a reference. f: " f " - event: " (class evt)))
      (swap! x f evt)))
  ([f x]
    (partial (event-handler f) x)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utils

(def genid
  "Generates a unique id string."
  #(name (gensym)))

(defmacro with-id
  "Assigns a unique id to the component which can be
used in the component's definition (e.g. in event handlers)."
  [x component]
  `(let [~x (genid)]
    (assoc-in (#'lab.ui.core/hiccup->component ~component) [:attrs :id] ~x)))
