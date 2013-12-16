(ns lab.ui.swing.event
  (:require [lab.ui.protocols :as p]
            [lab.ui.core :as ui])
  (:import [javax.swing UIManager JComponent AbstractAction]
           [java.awt Dimension]
           [java.awt.event InputEvent
                           KeyEvent
                           MouseEvent
                           FocusEvent
                           ActionEvent]))

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
    (mapv first)))

(defn- merge-results
  "Takes two functions that returns a map and 
merges both in a single one."
  [f1 f2]
  #(merge (f1 %) (f2 %)))

(defn- merge-impls
  "Given two protocol map implementations, it takes x and
overrides the implementations present in y."
  [x y]
  (reduce 
    (fn [x k] (update-in x [k] merge-results (k y)))
    x
    (keys y)))

(defn- build-merged-impl
  "Receives maps with Event protocol implementations
and merges the map returned by the to-map function.
TODO: provide a map with merging functions."
  [& impls]
  (reduce merge-impls impls))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Event Object

(def event-object "Root implementation."
  {:to-map (fn [this] {:source (p/abstract (.getSource this))})})

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
  {:to-map (fn [this] {:modifiers (flag-modifiers input-modifiers (.getModifiers this))})})

(def key-event
  {:to-map (fn [this]
             {:char         (.getKeyChar this)
              :code         (.getKeyCode this)
              :description  (KeyEvent/getKeyText (.getKeyCode this))
              :event        (key-event-ids (.getID this))})})

(extend KeyEvent
  p/Event
  (build-merged-impl event-object input-event key-event))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Mouse Event

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
  {:to-map (fn [this]
             {:button       (mouse-button (.getButton this))
              :click-count  (.getClickCount this)
              :screen-loc   (as-> (.getLocationOnScreen this) p [(.getX p) (.getY p)])
              :point        (as-> (.getPoint this) p [(.getX p) (.getY p)])
              :event        (mouse-event-ids (.getID this))})})

(extend MouseEvent
  p/Event
  (build-merged-impl event-object input-event mouse-event))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Action Event

(def action-modifiers
  {:alt   ActionEvent/ALT_MASK
   :ctrl  ActionEvent/CTRL_MASK
   :shift ActionEvent/SHIFT_MASK})

(def action-event
  {:to-map (fn [this]
             {:source    (p/abstract (.getSource this))
              :modifiers (flag-modifiers action-modifiers (.getModifiers this))})})

(extend ActionEvent
  p/Event
  (build-merged-impl event-object action-event))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Focus Event

(def focus-event
  {:to-map (fn [this]
             {:previous  (.getOppositeComponent this)
              :temporary (.isTemporary this)})})

(extend FocusEvent
  p/Event
  (build-merged-impl event-object focus-event))
