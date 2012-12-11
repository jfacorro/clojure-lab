(ns macho.ui.protocols)
;;-----------------------------------------
(defprotocol UIComponent
  "UI control abstraction"
  (add! [ctrl child] "Add a child control to ctrl.")
  (set-attr! [ctrl k v] "Sets the value for attr.")
  (show! [ctrl] "Show the control.")
  (hide! [ctrl] "Hides the control.")
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