(ns lab.ui.swing.misc-control
  (:import [javax.swing JButton JLabel]
           [java.awt.event ActionListener])
  (:require [lab.ui.protocols :as p]
            [lab.ui.swing.util :as util]))

(util/definitializations
  :button      JButton
  :label       JLabel)
  
(util/defattributes
  :button
    (:icon [c _ img]
      (.setIcon (p/impl c) (util/icon img)))
    (:on-click [c _ f]
      (let [action (reify ActionListener
                      (actionPerformed [this e] (f e)))]
        (.addActionListener (p/impl c) action))))