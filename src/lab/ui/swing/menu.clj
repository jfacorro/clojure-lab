(ns lab.ui.swing.menu
  (:import  [javax.swing JMenuBar JMenu JMenuItem JSeparator]
            [java.awt.event ActionListener])
  (:use     [lab.ui.protocols :only [impl]])
  (:require [lab.ui.core :as ui]
            [lab.ui.swing.util :as util]))

(ui/definitializations
  ;; Menu
  :menu-bar    JMenuBar
  :menu        JMenu
  :menu-item   JMenuItem
  :menu-separator JSeparator)
  
(ui/defattributes
  :menu-item
    (:on-click [c _ f]
      (let [action (reify ActionListener
                      (actionPerformed [this e] (f e)))]
        (.addActionListener (impl c) action)))
    (:key-stroke [c _ ks]
      (.setAccelerator (impl c) (util/key-stroke ks))))