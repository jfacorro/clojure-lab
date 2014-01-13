(ns lab.ui.core
  "Provides the API to create and manipulate UI component 
abstractions as Clojure records. Components can be declared as 
maps or in a hiccup format. Existing tags are defined in an ad-hoc 
hierarchy which can be extended as needed.

Implementation of components is based on the definitialize and defattribute
multi-methods. The former should return an instance of the underlying UI object,
while the latter is used for setting its attributes' value defined in the 
abstract specification (or explicitly through the use of `attr`).

Example: the following code creates a 300x400 window with a \"Hello!\" button
         and shows it on the screen.

  (-> [:window {:size [300 400]} [:button {:text \"Hello!\"}]]
    init
    show)"
  (:refer-clojure :exclude [find remove])
  (:require [clojure.zip :as zip]
            [lab.util :as util]
            [lab.ui.protocols :as p]
            [lab.ui.select :as sel]
            [lab.ui.hierarchy :as h]))

(declare init initialized? attr find genid selector# hiccup->component)

(def ui-action-macro 
  "This var should be set by the UI implementation with a macro 
that runs code in the UI thread.")

(defmacro action
  "Macro that uses the UI aciton macro defined by the implementation."
  [& body]
  `(~ui-action-macro ~@body))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Convenience macros for multimethod implementations

(defmacro definitializations
  "Generates all the multimethod implementations
for each of the entries in the map destrcutured
from its args.
  
  :component-name ClassName or init-fn"
  [& {:as m}]
  `(do
    ~@(for [[k c] m]
      (if (and (not (seq? c)) (-> c resolve class?))
        `(defmethod p/initialize ~k [c#]
          (new ~c))
        `(defmethod p/initialize ~k [x#]
          (~c x#))))))

(defmacro defattributes
  "Convenience macro to define attribute setters for each component type. 

The method implemented returns the first argument (which is the component 
itself), UNLESS the `^:modify` metadata flag is true for the argument vector, 
in which case the value from the last expression in the body is returned.

  *attrs-declaration
  
Where each attrs-declaration is:

  component-tag *attr-declaration
    
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
                    (assert (not= c '_) "First arg symbol can't be _")
                    `(defmethod p/set-attr [~tag ~attr]
                      ~args
                      (let [~x (do ~@body)]
                        ~(if (-> args meta :modify) x c))))))]
    `(do ~@(mapcat (partial apply f) comps))))

(defn- update-abstraction
  "Takes a component and set its own value 
as the abstraction of its implementation."
  [c]
  (let [impl (p/abstract (p/impl c) c)]
    (p/impl c impl)))

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
    (let [id (p/selected (p/impl this))]
      (find this (selector# id))))
  (selected [this selected]
    (p/selected (p/impl this) (p/impl selected)))

  Object
  (toString [this]
    (if-let [id (attr this :id)]
      (str tag " (#" id ")")
      tag))

  p/TextEditor
  (apply-style [this tokens styles]
    (p/apply-style (p/impl this) tokens styles)))

;; Have to use this since remove is part of the java.util.Map interface.
(extend-type UIComponent
  p/Component 
  (children [this]
    (:content this))
  (add [this child]
    (let [this  (init this)
          child (init child)]
      (-> this
        (update-in [:content] conj child)
        update-abstraction
        (p/impl (p/add (p/impl this) (p/impl child))))))
  (remove [this child]
    (let [i (.indexOf ^java.util.List (p/children this) child)]
      (-> this
        (update-in [:content] util/remove-at i)
        update-abstraction
        (p/impl (p/remove (p/impl this) (p/impl child))))))
  (add-binding [this ks f]
    (let [this    (init this)
          implem  (p/impl this)]
      (p/impl this (p/add-binding implem ks f))))
  (remove-binding [this ks]
    (let [implem (p/impl this)]
      (p/impl this (p/remove-binding implem ks)))))
      
(defn remove-all
  "Takes a component and removes all of its children."
  [c]
  (reduce p/remove c (p/children c)))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Expose Protocol Functions

(def children #'p/children)
(def add #'p/add)
(def remove #'p/remove)

(def apply-style #'p/apply-style)

(def selected #'p/selected)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Private supporting functions

(defn- component?
  "Returns true if x is a component."
  [x]
  (or (instance? UIComponent x)
      (and (vector? x)
           (isa? h/hierarchy (first x) :component))))

(defn- attrs?
  "Returns true if x is an attribute map."
  [x]
  (and (map? x) (not (component? x))))

(defn- attr->component [attrs [k v]]
  (if (component? v)
    (update-in attrs [k] hiccup->component)
    attrs))

(defn- hiccup->component
  "Used to convert huiccup syntax declarations to map components.
x should be a vector with the content [tag-keyword attrs-map? children*]"
  [x]
  (if (vector? x)
    (let [[tag & [attrs & ch :as children]] x]
      (->UIComponent
        tag
        (if (attrs? attrs)
          (reduce attr->component attrs attrs)
          {})
        (mapv hiccup->component (if-not (attrs? attrs) children ch))))
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
            (attr c k (if (component? v) (init v) v)))]
    (reduce f component attrs)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initialization and Attributes Access

(defn attr
  "Uses the set-attr multimethod to set the attribute value 
for the implementation and updates the abstract component
as well."
  ([c k]
    (if-let [v (get-in c [:attrs k])]
      v
      nil))
  ([c k v]
    (-> c
      (assoc-in [:attrs k] v)
      (as-> c
        (if (initialized? c)
          (-> c (p/set-attr k v) update-abstraction)
          c)))))

(defn- check-missing-id
  "Makes sure the component is not
missing an :id attribute."
  [c]
  (if (attr c :id)
    c
    (attr c :id (genid))))

(defn- run-post-init
  "Checks if there's a :post-init attribute
and applies it to the component."
  [c]
  (if-let [post-init (attr c :post-init)]
    (post-init c)
    c))

(defn init
  "Initializes a component, creating the implementation for 
each child and the attributes that have a component as a value."
  [c]
  {:post [(component? c)]}
  (let [c (hiccup->component c)]
    (if (initialized? c) ; do nothing if it's already initiliazed
      c
      (-> c
        (p/impl (p/initialize c))
        set-attrs
        init-content
        check-missing-id
        run-post-init
        update-abstraction))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Finding and Updating

(defn find
  "Returns the first component found."
  [root selector]
  (when-let [path (sel/select root selector)]
    (get-in root path)))

(defn selector#
  "Builds an id selector."
  [^String id]
  (-> (str "#" id) keyword))

(defn parent
  "Selects the parent of the component with the id."
  [id]
  (fn [c]
    (when c (some (sel/id= id) (children c)))))

(def ^:private zipper (partial zip/zipper map? :content identity))

(def ^:private attr-spec
  {:path  (fn [k] [:attrs k])
   :alts  (fn [x]
            (->> (:attrs x)
              (filter (comp component? val))
              (map (juxt (comp zipper val) key))))})

(defn update
  "Updates all the components that match the selector expression
using (update-in root path-to-component f args)."
  [root selector f & args]
  (reduce (fn [x path]
            (if (empty? path)
              (apply f x args)
              (apply update-in x path f args)))
          root
          (sel/select-all root selector attr-spec)))

(defn update!
  "Same as update but assumes root is an atom."
  [root selector f & args]
  {:pre [(instance? clojure.lang.Atom root)]}
  (apply swap! root update selector f args))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utils

(def genid
  "Generates a unique id string."
  #(name (gensym "component")))

(defmacro with-id
  "Assigns a unique id to the component which can be
used in the component's definition (e.g. in event handlers)."
  [x component]
  `(let [~x (genid)]
    (assoc-in (#'lab.ui.core/hiccup->component ~component) [:attrs :id] ~x)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Stylesheets

(defn- apply-class
  [c [selector attrs]]
  (reduce (fn [c [attr-name value]]
            (update c selector attr attr-name value))
          c
          attrs))

(defn apply-stylesheet
  "Takes an atom with the root of a (initialized abstract UI) component and
  a stylesheet (map where the key is a selector and the values a map of attributes
  and values) and applies it to the matching components."
  [c stylesheet]
  (reduce apply-class (hiccup->component c) stylesheet))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Properties for all UI components implementation independent

(defattributes
  :component
    (:id [c _ v]
      (when (not= (attr c :id) v)
        (throw (Exception. (str "Can't change the :id once it is set: " c)))))
    (:post-init [c _ _]))
