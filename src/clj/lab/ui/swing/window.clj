(ns lab.ui.swing.window
  (:require [lab.ui.core :as ui]
            [lab.ui.util :refer [defattributes definitializations]]
            [lab.ui.swing.util :as util]
            [lab.ui.protocols :refer [impl Implementation listen ignore]])
  (:import  [javax.swing JFrame WindowConstants]
            [java.awt Window]
            [java.awt.event WindowAdapter]))

(extend-type JFrame
  Implementation
  (abstract
    ([this] nil)
    ([this the-abstract] this)))

(defn- window-init [c]
  (doto (JFrame.)
    (.setDefaultCloseOperation WindowConstants/DO_NOTHING_ON_CLOSE)))

(definitializations :window window-init)

(defattributes
  :window
  (:background [c _ v]
    (.. ^Window (impl c) (setBackground (util/color v))))
  (:opacity [c _ v]
    (.setOpacity ^Window (impl c) ^float v))
  (:fullscreen [c _ v]
    (util/fullscreen (when v (impl c))))
  (:title [c _ v]
    (.setTitle ^JFrame (impl c) v))
  (:maximized [c _ v]
    (when v
      (.setExtendedState ^JFrame (impl c) 
        (bit-or (.getExtendedState ^JFrame (impl c)) 
          JFrame/MAXIMIZED_BOTH))))
  (:size [c _ [w h]]
    (.setSize ^JFrame (impl c) w h))
  (:menu [c _ v]
    (.setJMenuBar ^JFrame (impl c) (impl v))
    (.revalidate ^JFrame (impl c)))
  (:icons [c _ v]
    (let [icons (map util/image v)]
      (.setIconImages ^JFrame (impl c) icons)))
  (:default-button [c _ v]
    (.. ^JFrame (impl c) getRootPane (setDefaultButton (impl v)))))

(defn- event-listener-helper [c evt f]
  (let [listener  (util/create-listener c evt f)]
    (.addWindowListener ^JFrame (impl c) listener)
    listener))

(defn- event-ignore-helper [c evt listener]
  (.removeWindowListener ^JFrame (impl c) listener))

(defmethod listen [:window :closed]
  [c evt f]
  (event-listener-helper c evt f))

(defmethod ignore [:window :closed]
  [c evt listener]
  (event-ignore-helper c evt listener))

(defmethod listen [:window :closing]
  [c evt f]
  (event-listener-helper c evt f))

(defmethod ignore [:window :closing]
  [c evt listener]
  (event-ignore-helper c evt listener))

(defmethod listen [:window :opened]
  [c evt f]
  (event-listener-helper c evt f))

(defmethod ignore [:window :opened]
  [c evt listener]
  (event-ignore-helper c evt listener))

(defmethod listen [:window :minimized]
  [c evt f]
  (event-listener-helper c evt f))

(defmethod ignore [:window :minimized]
  [c evt listener]
  (event-ignore-helper c evt listener))

(defmethod listen [:window :restored]
  [c evt f]
  (event-listener-helper c evt f))

(defmethod ignore [:window :restored]
  [c evt listener]
  (event-ignore-helper c evt listener))
