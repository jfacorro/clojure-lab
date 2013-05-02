(ns lab.ui.protocols
  (:refer-clojure :exclude [remove]))

(defprotocol Component
  (add [this child] "Add a child to a component. Must return the parent with the child added.")
  (remove [this child] "Removes child from the children collection."))

(defprotocol Visible
  (visible? [this])
  (hide [this])
  (show [this]))

(defprotocol Window
  (maximize [this])
  (restore [this])
  (minimze [this]))

(defprotocol Abstract
  (impl [this] [this implementation] "Gets or sets the implementation for component."))

(defprotocol Selected
  (get-selected [this])
  (set-selected [this selected]))

(defmulti initialize
  "Creates a component instance based on its :tag."
  :tag)

(defmulti set-attr
  "Sets the attribute value for this component and returns the
  modified component."
  (fn [{tag :tag} k _]
    [tag k]))

