(ns lab.ui.swing.event
  (:require [lab.ui.protocols :as p]
            [lab.ui.swing.keys :refer [swing-keys]]
            [lab.ui.core :as ui]
            lab.util)
  (:import [javax.swing UIManager JComponent AbstractAction]
           [javax.swing.event DocumentEvent DocumentEvent$EventType CaretEvent]
           [java.awt Dimension]
           [java.awt.event InputEvent
                           KeyEvent
                           MouseEvent MouseWheelEvent
                           FocusEvent
                           ActionEvent
                           WindowEvent]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Event hijacking

(def event-types
  {java.awt.event.MouseEvent      :mouse-event
   java.awt.event.KeyEvent        :key-event
   java.awt.event.FocusEvent      :focus-event
   java.awt.event.WindowEvent     :window-event})

(defn event-type
  "Goes through the map's keys looking for the class
to which the event belongs.
TODO: improve implementation to be able to add parent classes as keys."
  [e m]
  (->> m
    (filter #(instance? (key %) e))
    first
    second))

(defmacro hijack-events
  "Takes a class and a map that defines the event
types to be hijacked with the handlers to be used.
Event handlers get an event in the form of a map 
(through the use of to-map) and a thunk with a call to the
super's implementation of processEvent(...).

(defn f [x]
  (fn [e super]
    (println x \"=>\" e)
    (super)))

(def handlers
  {:key-event    (#'f \"key\")
   :focus-event  (#'f \"focus\")})

Returns an instance of the object with the processEvent(...)
methods overriden. "
  [clazz handlers]
  `(proxy [~clazz] []
    (processEvent [e#]
      (let [event-type# (event-type e# event-types)
            handler#    (~handlers event-type#)
            super#      #(proxy-super processEvent e#)]
        (if handler#
          (handler# (p/to-map e#) super#)
          (super#))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utility functions

(defn- flag-modifiers
  "Takes a map and an integer representing a flag value, 
and returns a vector with all the values that apply to it."
  [m n]
  (->> m 
    (filter #(pos? (bit-and n (second %))))
    (map first)
    set))

(defn- merge-results
  "Takes two functions that return a map and 
merges both in a single one."
  [f1 f2]
  #(merge (f1 %) (f2 %)))

(defn- merge-impls
  "Given two protocol map implementations, it takes x and
overrides the implementations present in y."
  [ks x y]
  (reduce 
    (fn [x k]
      (if (contains? ks k)
        (update-in x [k] merge-results (k y))
        (assoc x k (k y))))
    x
    (keys y)))

(defn- build-merged-impl
  "Receives maps with Event protocol implementations
and merges the implementation maps returned by the functions
with key in ks. Other functions are just replaced with their 
latest version."
  [ks & impls]
  (reduce (partial merge-impls ks) impls))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Event Object

(defn- consume
  "Default implementation for consume that does nothing."
  [e]
  (throw (UnsupportedOperationException. "Can't consume this type of event.")))

(def event-object "Root implementation."
  {:to-map (fn [^java.util.EventObject this]
             (-> {:source (p/abstract (.getSource this))}
               p/map->UIEvent
               (p/impl this)))
   :consume consume})

(extend java.util.EventObject
  p/Event
  event-object)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Key Event

(def ^:private key-event-ids
  {KeyEvent/KEY_PRESSED  :pressed
   KeyEvent/KEY_RELEASED :released
   KeyEvent/KEY_TYPED    :typed})

(def ^:private input-modifiers
  {:alt    InputEvent/ALT_MASK    
   :shift  InputEvent/SHIFT_MASK
   :ctrl   InputEvent/CTRL_MASK})

(def input-event
  {:to-map  (fn [^InputEvent this] {:modifiers (flag-modifiers input-modifiers (.getModifiers this))})
   :consume (fn [^InputEvent this] (.consume this))})

(def key-event
  {:to-map (fn [^KeyEvent this]
             {:char         (.getKeyChar this)
              :code         (.getKeyCode this)
              :description  (swing-keys (.getKeyCode this))
              :event        (key-event-ids (.getID this))})})

(extend KeyEvent
  p/Event
  (build-merged-impl #{:to-map} event-object input-event key-event))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Mouse Events

(def ^:private mouse-button
  {MouseEvent/BUTTON1  :button-1
   MouseEvent/BUTTON2  :button-2
   MouseEvent/BUTTON3  :button-3})

(def ^:private mouse-event-ids
  {MouseEvent/MOUSE_CLICKED  :clicked
   MouseEvent/MOUSE_DRAGGED  :dragged
   MouseEvent/MOUSE_ENTERED  :entered
   MouseEvent/MOUSE_EXITED   :exited
   MouseEvent/MOUSE_MOVED    :moved
   MouseEvent/MOUSE_PRESSED  :pressed
   MouseEvent/MOUSE_RELEASED :released
   MouseEvent/MOUSE_WHEEL    :wheel})

(def mouse-event
  {:to-map (fn [^MouseEvent this]
             {:button       (mouse-button (.getButton this))
              :click-count  (.getClickCount this)
              :screen-loc   (as-> (.getLocationOnScreen this) p [(.getX p) (.getY p)])
              :point        (as-> (.getPoint this) p [(.getX p) (.getY p)])
              :event        (mouse-event-ids (.getID this))})})

(def mouse-wheel-event
  {:to-map (fn [^MouseWheelEvent this]
             {:wheel-rotation  (.getWheelRotation this)
              :scroll-amount   (.getScrollAmount this)})})

(extend MouseEvent
  p/Event
  (build-merged-impl #{:to-map} event-object input-event mouse-event))

(extend MouseWheelEvent
  p/Event
  (build-merged-impl #{:to-map} event-object input-event mouse-event mouse-wheel-event))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Action Event

(def action-modifiers
  {:alt   ActionEvent/ALT_MASK
   :ctrl  ActionEvent/CTRL_MASK
   :shift ActionEvent/SHIFT_MASK})

(def action-event
  {:to-map (fn [^ActionEvent this]
             {:source    (p/abstract (.getSource this))
              :modifiers (flag-modifiers action-modifiers (.getModifiers this))})})

(extend ActionEvent
  p/Event
  (build-merged-impl #{:to-map} event-object action-event))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Focus Event

(def focus-event
  {:to-map (fn [^FocusEvent this]
             {:event     :focus
              :previous  (.getOppositeComponent this)
              :temporary (.isTemporary this)})})

(extend FocusEvent
  p/Event
  (build-merged-impl #{:to-map} event-object focus-event))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Caret Event

(def caret-event
  {:to-map (fn [^CaretEvent this]
             {:event     :caret
              :position  (.getDot this)
              :end       (.getMark this)})})

(extend CaretEvent
  p/Event
  (build-merged-impl #{:to-map} event-object caret-event))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Document Event

(def ^:private document-event-types
  {DocumentEvent$EventType/INSERT  :insert
   DocumentEvent$EventType/REMOVE  :remove
   DocumentEvent$EventType/CHANGE  :change})

(extend-protocol p/Event
  DocumentEvent
  (to-map [this]
    (let [offset     (.getOffset this)
          length     (.getLength this)
          doc        (.getDocument this)
          editor     (.getProperty doc :component)
          event-type (document-event-types (.getType this))
          text       (when (not= event-type :remove)
                       (.getText doc offset length))]
      {:source   (p/abstract editor)
       :offset   offset
       :length   length
       :text     text
       :type     event-type
       :document doc})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Window Event

(extend WindowEvent
  p/Event
  event-object)
