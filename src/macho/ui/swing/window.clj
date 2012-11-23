(ns macho.ui.swing.window
  (:import [javax.swing JFrame])
  (:use [macho.ui.protocols]
        [macho.ui.swing.component]))

(defn window [title]
  (JFrame. title))

(defmethod comp-set-attr! :icon [wdw _ img]
  "Sets the size of the window."
  (.setIconImage wdw img))

(defn icon! [w img]
  "Sets the size of the window."
  (.setIconImage w img))

(defn icons! [w imgs]
  "Sets the size of the window."
  (.setIconImages w imgs))

(defn icon [w]
  "Gets the size of the window."
  (.getIconImage w))