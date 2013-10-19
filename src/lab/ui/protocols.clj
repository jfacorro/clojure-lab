(ns lab.ui.protocols
  (:refer-clojure :exclude [remove])
  (:require [lab.ui.hierarchy :as h]))

(defprotocol Component
  (children [this] "Gets all the children for the component.")
  (add [this child] "Add a child to a component. Must return the parent with the child added.")
  (remove [this child] "Removes child from the children collection.")
  (add-binding [this ks f] "Adds a key binding to this component.")
  (remove-binding [this ks] "Removes a key binding from this component."))

(defprotocol Visible
  (visible? [this])
  (hide [this])
  (show [this]))

(defprotocol Abstract
  (impl [this] [this implementation] "Gets or sets the implementation for component."))

(defprotocol Implementation
  (abstract [this] [this the-abstract] "Gets or sets the asbtract component for the implementation."))

(defprotocol Selected
  (selected [this] [this selected] "Gets or sets the selected children abstract component."))

(defprotocol Event
  (event->map [this] "Serializes the event into a map.")
  (source [this] "Gets the component that generated the event."))

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

