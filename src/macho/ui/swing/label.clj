(ns macho.ui.swing.label
  (:import [javax.swing JLabel]))

(defn label [s]
  (JLabel. s))