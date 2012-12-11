(ns macho.ui.swing.util
  (:import [javax.swing SwingUtilities]))
  
(defn queue-action
  "Queues an action to the event queue."
  [f]
  (SwingUtilities/invokeLater f))