(ns lab.ui.swing.menu
  (:use     [lab.ui.protocols :only [impl to-map]])
  (:require [lab.ui.core :as ui]
            [lab.ui.swing.util :as util])
  (:import  [javax.swing JMenuBar JMenu JMenuItem JSeparator]
            [java.awt.event ActionListener]))

(ui/definitializations
  ;; Menu
  :menu-bar    JMenuBar
  :menu        JMenu
  :menu-item   JMenuItem
  :menu-separator JSeparator)
  
(ui/defattributes
  :menu
  (:text [c _ v]
    (.setText ^JMenu (impl c) v))

  :menu-item
  (:text [c _ v]
    (.setText ^JMenuItem (impl c) v))
  (:on-click [c _ f]
    (let [action (reify ActionListener
                   (actionPerformed [this e] (ui/handle-event f e)))]
      (.addActionListener ^JMenuItem (impl c) action)))
  (:keystroke [c _ ks]
    (.setAccelerator ^JMenuItem (impl c) (util/keystroke ks))))
