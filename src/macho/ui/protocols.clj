(ns macho.ui.protocols)
;;-----------------------------------------
;; Protocols
;;-----------------------------------------
(defprotocol Visible
  "Control that can be shown or hidden."
  (show [this] [this args] "Show the control.")
  (hide [this] "Hide the control."))
;;-----------------------------------------
(defprotocol Composite
  "Control that can be built from the aggregation of other controls."
  (add [this child] [this child args] "Add a child control to ctrl."))
;;-----------------------------------------
(defprotocol TextEdit
  "Text edit (insertion or removal)."
  (insertion? [this] "Evaluates if this is an insertion.")
  (offset [this] "Gets the offset at which the edit took place."))
;;-----------------------------------------
(defprotocol Text
  "Is a piece of text."
  (length [this] "Gets the length for the content.")
  (text [this] "Gets the text content for this entity."))
;;-----------------------------------------
;; Multi-methods
;;-----------------------------------------
(defmulti on
  "Multimethod for the binding of events."
  #(first %&) :default :none)
;;-----------------------------------------