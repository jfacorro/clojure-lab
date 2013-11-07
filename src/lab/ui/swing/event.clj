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
;; Key Event

(def ^:private key-event-ids
  {KeyEvent/KEY_PRESSED  :pressed
   KeyEvent/KEY_RELEASED :released
   KeyEvent/KEY_TYPED    :typed})

(def ^:private input-modifiers
  {:alt    InputEvent/ALT_MASK    
   :shift  InputEvent/SHIFT_MASK
   :ctrl   InputEvent/CTRL_MASK})

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Action Event

(def action-modifiers
  {:alt   ActionEvent/ALT_MASK
   :ctrl  ActionEvent/CTRL_MASK
   :shift ActionEvent/SHIFT_MASK})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utility functions

(defn- flag-modifiers [m n]
  (->> m 
    (filter #(pos? (bit-and n (second %))))
    (mapv first)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Event protocol implementation

(extend-protocol p/Event
  java.util.EventObject
  (to-map [this]
    {:source (p/abstract (.getSource this))})
  KeyEvent
  (to-map [this]
    {:source       (p/abstract (.getSource this))
     :char         (.getKeyChar this)
     :code         (.getKeyCode this)
     :description  (KeyEvent/getKeyText (.getKeyCode this))
     :event        (key-event-ids (.getID this))
     :modifiers    (flag-modifiers input-modifiers (.getModifiers this))})
  MouseEvent
  (to-map [this]
    {:source       (p/abstract (.getSource this))
     :button       (mouse-button (.getButton this))
     :click-count  (.getClickCount this)
     :screen-loc   (as-> (.getLocationOnScreen this) p [(.getX p) (.getY p)])
     :point        (as-> (.getPoint this) p [(.getX p) (.getY p)])
     :event        (mouse-event-ids (.getID this))
     :modifiers    (flag-modifiers input-modifiers (.getModifiers this))})
  FocusEvent
  (to-map [this]
    {:source    (p/abstract (.getSource this))
     :previous  (.getOppositeComponent this)
     :temporary (.isTemporary this)})
  ActionEvent
  (to-map [this]
    {:source    (p/abstract (.getSource this))
     :modifiers (flag-modifiers action-modifiers (.getModifiers this))}))
