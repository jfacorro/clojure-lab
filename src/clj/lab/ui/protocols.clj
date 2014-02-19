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

(defprotocol Selection
  (selection [this] [this selection]
    "Gets or sets the selection of an abstract component."))

(defprotocol Event
  (to-map [this] "Serializes the event into a map.")
  (consume [this] "Consumes this event, preventing it from bubbling up."))

(defprotocol StyledTextEditor
  (apply-style [this tokens styles] [this start len style]
    "Applies a formatting style to the region defined by start and length."))

(defprotocol TextEditor
  (add-highlight [this start end color] "Adds a highlight of the specified color from start to end and returns an identifier of the highlight.")
  (remove-highlight [this id] "Removed the highlight of the given id.")
  (caret-position [this] [this position] "Gets and sets the caret position for this text component.")
  (goto-line [this n] "Positions the caret at the beggining of line n."))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Multi methods

(defn tag-key-dispatch [{:keys [tag]} k & _]
  [tag k])

(defmulti initialize
  "Creates a component instance based on its :tag."
  :tag
  :hierarchy #'h/hierarchy)

(defmulti set-attr
  "Sets the attribute value for this component and returns the
  modified component."
  tag-key-dispatch
  :hierarchy #'h/hierarchy)

(defmulti listen
  "Add an event handler for the event specified."
  tag-key-dispatch
  :hierarchy #'h/hierarchy)

(defmulti ignore
  "Remove an event handler for the event specified."
  tag-key-dispatch
  :hierarchy #'h/hierarchy)
