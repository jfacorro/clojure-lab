(ns lab.ui.swing.menu
  (:use     [lab.ui.protocols :only [impl to-map listen ignore]])
  (:require [lab.ui.core :as ui]
            [lab.ui.util :refer [defattributes definitializations]]
            [lab.ui.swing.util :as util])
  (:import  [javax.swing JMenuBar JMenu JMenuItem JSeparator JPopupMenu]
            [java.awt.event ActionListener]))

(definitializations
  ;; Menu
  :menu-bar    JMenuBar
  :menu        JMenu
  :menu-item   JMenuItem
  :menu-separator JSeparator
  :pop-up-menu JPopupMenu)
  
(defattributes
  :menu
  (:text [c _ v]
    (.setText ^JMenu (impl c) v))

  :pop-up-menu
  (:visible [c _ v]
    (when v
      (let [invoker (impl (ui/attr c :source))
            [x y]   (ui/attr c :location)]
        (.show (impl c) invoker x y))))
  (:source [c _ v]
    (.setInvoker (impl c) (impl v)))

  :menu-item
  (:text [c _ v]
    (.setText ^JMenuItem (impl c) v))
  (:keystroke [c _ ks]
    (.setAccelerator ^JMenuItem (impl c) (util/keystroke ks))))

(defmethod listen [:menu-item :click]
  [c evt f]
  (let [listener  (util/create-listener c evt f)]
    (.addActionListener ^JMenuItem (impl c) listener)
    listener))

(defmethod ignore [:menu-item :click]
  [c _ listener]
  (.removeActionListener ^JMenuItem (impl c) listener))
