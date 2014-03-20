(ns lab.ui.swing.misc-control
  (:require [lab.ui.core :as ui]
            [lab.ui.util :refer [defattributes definitializations]]
            [lab.ui.protocols :as p]
            [lab.ui.swing.util :as util])
  (:import [javax.swing JButton JLabel JCheckBox]))

(definitializations
  :button      JButton
  :label       JLabel
  :checkbox    JCheckBox)

(defattributes
  :button
  (:text [c _ v]
    (.setText ^JButton (p/impl c) v))
  (:transparent [c _ v]
    (.setContentAreaFilled ^JButton (p/impl c) (not v)))
  (:icon [c _ img]
    (.setIcon ^JButton (p/impl c) (util/icon img)))

  :label
  (:text [c _ v]
    (.setText ^JLabel (p/impl c) v))
  
  :checkbox
  (:text [c _ v]
    (.setText ^JCheckBox (p/impl c) v)))

(extend-type JCheckBox
  p/Selection
  (p/selection
    ([this]
      (.isSelected this))
    ([this v]
      (.setSelected this v))))

(defmethod p/listen [:button :click]
  [c evt f]
  (let [listener  (util/create-listener c evt f)]
    (.addActionListener ^JButton (p/impl c) listener)
    listener))

(defmethod p/ignore [:button :click]
  [c _ listener]
  (.removeActionListener ^JButton (p/impl c) listener))
