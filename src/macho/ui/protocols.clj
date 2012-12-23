(ns macho.ui.protocols)
;;-----------------------------------------
(defprotocol UIComponent
  "UI control abstraction"
  (add! [this child] "Add a child control to ctrl.")
  (set-attr! [this attr value] "Sets the value for attr.")
  (show! [this] "Show the control.")
  (hide! [this] "Hide the control.")
  ;;(on [ctrl evt f] "Binds the fn to the event.")
)
;;-----------------------------------------
(def ^"Gets the 2nd parameter from the args list." specified-key #(second %&))
;;-----------------------------------------
(defmulti comp-set-attr!
  "Used for implementing controls' setters."
  specified-key :default :none)
;;-----------------------------------------
(defmulti on
  "Multimethod for the binding of events."
  #(first %&) :default :none)
;;-----------------------------------------
(defprotocol TextEdit
  "Represents a text edit (insertion or removal)."
  (insertion? [this] "Evaluates if e is an insertion.")
  (offset [this] "Gets the offset at which the edit took place."))

(defprotocol Text
  "Represents a text edit (insertion or removal)."
  (length [this] "Gets the length for the content.")
  (text [this] "Gets the text content for this entity."))