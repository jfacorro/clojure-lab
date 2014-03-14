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
            [lab.model.protocols :as mp]
            [lab.ui.protocols :as p]
            [lab.ui.select :as sel]
            [lab.ui.hierarchy :as h]
            [lab.ui.util :refer [defattributes definitializations]]))

(declare init initialized? attr find genid selector# hiccup->component)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UI action macro

(defn- action-macro
  "This var should be set by the UI implementation with a macro 
that runs code in the UI thread."
  [& xs]
  (throw (Exception. "action-macro has not been set by the implementation.")))

(defmacro action
  "Macro that uses the UI action macro defined by the implementation."
  [& body]
  `(~action-macro ~@body))

(defn register-action-macro!
  "Registration of macro to be used to execute 
code in the UI's implementation context."
  [x]
  (intern 'lab.ui.core 'action-macro x))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Event handler creation

(defn- event-handler
  "Default implementation for event handler, just passes the event
over to the the handler function."
  [f e]
  (f e))

(defn handle-event
  "Used in the implementation's event handlers."
  [f e]
  (event-handler f (p/to-map e)))

(defn register-event-handler!
  "Available function to customize event handling."
  [f]
  (intern 'lab.ui.core 'event-handler f))

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

  p/Selection
  (selection [this]
    (p/selection (p/impl this)))
  (selection [this selection]
    (p/selection (p/impl this) selection)
    this)

  Object
  (toString [this]
    (if-let [id (attr this :id)]
      (str tag " (#" id ")")
      tag))

  p/StyledTextEditor
  (apply-style [this tokens styles]
    (p/apply-style (p/impl this) tokens styles)
    this)
  (apply-style [this start len style]
    (p/apply-style (p/impl this) start len style)
    this)

  p/TextEditor
  (add-highlight [this start end color]
    (p/add-highlight (p/impl this) start end color))

  (remove-highlight [this id]
    (p/remove-highlight (p/impl this) id))

  (caret-position [this]
    (p/caret-position (p/impl this)))
  (caret-position [this position]
    (p/caret-position (p/impl this) position)
    this)
  (caret-location [this]
    (p/caret-location (p/impl this)))
  (goto-line [this n]
    (p/goto-line (p/impl this) n)))

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
    (let [i (util/index-of (p/children this) child)]
      (-> this
        (update-in [:content] util/remove-at i)
        update-abstraction
        (p/impl (p/remove (p/impl this) (p/impl child))))))  
  (focus [this]
    (p/focus (p/impl this))
    this)

  mp/Text
  (insert [this offset s]
    (mp/insert (p/impl this) offset s)
    this)
  (delete [this start end]
    (mp/delete (p/impl this) start end)
    this)
  (length [this]
    (mp/length (p/impl this)))
  (text [this]
    (mp/text (p/impl this)))
  (substring [this start end]
    (mp/substring (p/impl this) start end)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Abstract protocol implementation for nil

(extend-type nil
  p/Abstract
  (impl
    ([this] nil)
    ([this x] nil)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UI event abstraction

(defrecord UIEvent [source event]
  p/Event
  (to-map [this] this)
  (consume [this] (p/consume (p/impl this)))
  
  p/Abstract
  (impl [this] (-> this meta :impl))
  (impl [this x] (vary-meta this assoc :impl x)))

(defn memoized-key-stroke
  "Expects a keystroke event with keys :modifiers, :char and
:code."
  [{:keys [char modifiers code description] :as e}]
  (let [modif (->> (disj modifiers :shift) (map name) set)
        desc  (name description)
        ch    (str char)]
   [(conj modif ch) (conj modif desc)]))

(def key-stroke (memoize memoized-key-stroke))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Expose Protocol Functions

;; Component

(def children #'p/children)
(def add #'p/add)
(def remove #'p/remove)
(def focus #'p/focus)

(defn remove-all
  "Takes a component and removes all of its children."
  [c]
  (reduce p/remove c (p/children c)))

(defn add-all
  "Takes a component and removes all of its children."
  [c children]
  (reduce p/add c children))

;; Event Listener

(defn listeners [c event]
  (->> (meta c) :listen event (map first)))

(defn listen [c evt f]
  (let [listener   (p/listen c evt (partial handle-event f))
        ;; keywords can't have metadata so in case f is a keyword
        ;; wrap it in a vector in order to add the impl listener to it.
        f-meta     (with-meta [f] {:impl listener}) 
        listeners  (get-in (meta c) [:listen evt] #{})]
    (-> c
      (vary-meta assoc-in [:listen evt] (conj listeners f-meta))
      update-abstraction)))

(defn ignore [c evt f]
  (let [listener  (-> c listeners (get [f]) meta :impl)]
    (p/ignore c evt listener)
    (-> c
      (vary-meta assoc-in [:listen evt] dissoc [f])
      update-abstraction)))

(defn ignore-all [c evt]
  (let [listeners (->> (meta c) :listen evt)]
    (doseq [listener (map (comp :impl meta) listeners)]
      (p/ignore c evt listener))
    (-> c
      (vary-meta assoc-in [:listen evt] #{})
      update-abstraction)))

;; TextEditor

(def apply-style #'p/apply-style)
(def add-highlight #'p/add-highlight)
(def remove-highlight #'p/remove-highlight)
(def caret-position #'p/caret-position)
(def caret-location #'p/caret-location)
(def goto-line #'p/goto-line)

;; Selection

(def selection #'p/selection)

;; Events 

(def consume #'p/consume)

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
as well. It also gets the current attribute's value in the 
abstraction regardless of its value in the implementation.

TODO: define a way to get an updated value from the abstraction."
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

(defn update-attr
  "Applies the function f to the current value of attr k. The
result will be the new value of the attribute."
  [c k f & args]
  (attr c k (apply f (attr c k) args)))

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
  (if-let [f (attr c :post-init)]
    (handle-event f (map->UIEvent {:source c :event :post-init}))
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

(def attr= #'sel/attr=)

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
  {:pre [(instance? clojure.lang.IRef root)]}
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
    (:keymap [c _ v])
    (:post-init [c _ _])
    (:stuff [c _ _])
    (:listen ^:modify [c _ events]
      (assert (-> events count even?) "An even amount of items must be provided.")
      (reduce (fn [c [evt f]] (listen c evt f)) c (partition 2 events))))
