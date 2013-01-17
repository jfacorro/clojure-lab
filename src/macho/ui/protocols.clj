(ns macho.ui.protocols)
;;-----------------------------------------
;; Protocols
;;-----------------------------------------
(defprotocol Visible
  "Control that can be shown or hidden."
  (show [this] "Show the control.")
  (hide [this] "Hide the control."))
;;-----------------------------------------
(defprotocol Composite
  "Control that can be built from the aggregation of other controls."
  (add [this child] "Add a child control to ctrl.")
  (add [this child args] "Add a child control to ctrl."))
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
(def specified-key "Gets the 2nd parameter from the args list." #(second %&))

(defmulti on
  "Multimethod for the binding of events."
  #(first %&) :default :none)
;;-----------------------------------------