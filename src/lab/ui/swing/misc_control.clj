(ns lab.ui.swing.misc-control
  (:import [javax.swing JButton JLabel]
           [java.awt.event ActionListener])
  (:require [lab.ui.core :as ui]
            [lab.ui.protocols :as p]
            [lab.ui.swing.util :as util]))

(ui/definitializations
  :button      JButton
  :label       JLabel)

(ui/defattributes
  :button
  (:text [c _ v]
    (.setText ^JButton (p/impl c) v))
  (:transparent [c _ v]
    (.setContentAreaFilled ^JButton (p/impl c) (not v)))
  (:icon [c _ img]
    (.setIcon ^JButton (p/impl c) (util/icon img)))
  (:on-click [c _ f]
    (let [action (reify ActionListener
                    (actionPerformed [this e] (f (p/to-map e))))]
      (.addActionListener ^JButton (p/impl c) action)))
  :label
  (:text [c _ v]
    (.setText ^JLabel (p/impl c) v)))