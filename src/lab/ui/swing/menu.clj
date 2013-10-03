(ns lab.ui.swing.menu
  (:import  [javax.swing JMenuBar JMenu JMenuItem JSeparator]
            [java.awt.event ActionListener])
  (:use     [lab.ui.protocols :only [impl]])
  (:require [lab.ui.swing.util :as swutil]))


(swutil/definitializations
  ;; Menu
  :menu-bar    JMenuBar
  :menu        JMenu
  :menu-item   JMenuItem
  :menu-separator JSeparator)
  
(swutil/defattributes
  :menu-item
    (:on-click [c _ f]
      (let [action (reify ActionListener
                      (actionPerformed [this e] (f e)))]
        (.addActionListener (impl c) action)))
    (:key-stroke [c _ ks]
      (.setAccelerator (impl c) (swutil/key-stroke ks))))