(ns macho.ui.swing.core 
  (:import  [javax.swing UIManager])
  (:require [macho.ui.swing.component]
            [macho.ui.swing.util :as util]
            [macho.ui.protocols :as event]))

;;-------------------
;; Expose API
;;-------------------
(def queue-action util/queue-action)
(def on event/on)
;;-------------------
(defn- init []
  ;Set the application look & feel instead of Swings default.
  (UIManager/setLookAndFeel "javax.swing.plaf.nimbus.NimbusLookAndFeel")
  ;(UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName))
  ;(UIManager/setLookAndFeel (UIManager/getCrossPlatformLookAndFeelClassName))
  ;(UIManager/setLookAndFeel "com.sun.java.swing.plaf.motif.MotifLookAndFeel")
)
;;-------------------
(init)