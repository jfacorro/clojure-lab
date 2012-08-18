(ns macho.ui.protocols)

(defprotocol UIComponent
  "UI control abstraction"
  (add! [ctrl child] "Add a child control to ctrl.")
  (set-attr! [ctrl k v] "Sets the value for attr.")
  (show! [ctrl] "Show the control.")
  (hide! [ctrl] "Hides the control.")
  (on [ctrl evt fn] "Binds the fn to the event."))

(defn specified-key
  "Gets the 2nd parameter from the args list."
  [& xs] (nth xs 1))

(defmulti comp-set-attr!
  "Used for setting implementing controls' setters."
  specified-key :default :none)

(defmulti comp-on
  "Multimethod for the binding of events."
  specified-key :default :none)