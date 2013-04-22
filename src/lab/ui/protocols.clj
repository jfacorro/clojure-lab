(ns lab.ui.protocols)

(defprotocol Component
  (add [this child] "Add a child to a component. Must return the parent wuth the child added."))

(defprotocol Abstract
  (impl [this] [this implementation] "Gets or sets the implementation for component."))

(defprotocol Selected
  (get-selected [this])
  (set-selected [this selected]))

(defmulti create
  "Creates a component instance based on its :tag."
  :tag)

(defmulti set-attr
  "Sets the attribute value for this component and returns the
  modified component."
  (fn [{tag :tag} k _]
    [tag k]))

