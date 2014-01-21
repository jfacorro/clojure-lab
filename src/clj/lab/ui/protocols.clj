(ns lab.ui.protocols
  (:refer-clojure :exclude [remove])
  (:require [lab.ui.hierarchy :as h]))

(defprotocol Component
  (children [this] "Gets all the children for the component.")
  (add [this child] "Add a child to a component. Must return the parent with the child added.")
  (remove [this child] "Removes child from the children collection.")
  (focus [this] "Gives focus to this component."))

(defprotocol Abstract
  (impl [this] [this implementation] "Gets or sets the implementation for component."))

(defprotocol Implementation
  (abstract [this] [this the-abstract] "Gets or sets the asbtract component for the implementation."))

(defprotocol Selected
  (selected [this] [this selected]
    "Returns the id of the selected child or sets the selected child abstract component."))

(defprotocol Event
  (to-map [this] "Serializes the event into a map.")
  (consume [this] "Consumes this event, preventing it from bubbling up."))

(defprotocol TextEditor
  (apply-style [this tokens styles] [this start length style]
    "Applies a formatting style to the region defined by start and length.")
  (caret-position [this] [this position] "Gets and sets the caret position for this text component."))

;; Multi methods

(defmulti initialize
  "Creates a component instance based on its :tag."
  :tag
  :hierarchy #'h/hierarchy)

(defmulti set-attr
  "Sets the attribute value for this component and returns the
  modified component."
  (fn [{tag :tag} k _]
    [tag k])
  :hierarchy #'h/hierarchy)

