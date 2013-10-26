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
    (:transparent [c _ v]
      (.setContentAreaFilled (p/impl c) (not v)))
    (:icon [c _ img]
      (.setIcon (p/impl c) (util/icon img)))
    (:on-click [c _ f]
      (let [action (reify ActionListener
                      (actionPerformed [this e] (f e)))]
        (.addActionListener (p/impl c) action)))
  :label
  (:text [c _ v]
    (.setText (p/impl c) v)
    c))