(ns lab.ui.swing.window
  (:require [lab.ui.core :as ui]
            [lab.ui.swing.util :as util]
            [lab.ui.protocols :as p])
  (:import  [javax.swing JFrame WindowConstants]
            [java.awt.event WindowAdapter]))

(defn- window-init [c]
  (doto (JFrame.)
    (.setDefaultCloseOperation WindowConstants/DO_NOTHING_ON_CLOSE)))

(ui/definitializations :window window-init)

(ui/defattributes
  :window
    (:background [c _ v]
      (.. ^JFrame (p/impl c) getRootPane (setBackground (util/color v))))
    (:fullscreen [c _ v]
      (util/fullscreen (when v (p/impl c))))
    (:title [c _ v]
      (.setTitle ^JFrame (p/impl c) v))
    (:maximized [c _ v]
      (when v
        (.setExtendedState ^JFrame (p/impl c) 
                           (bit-or (.getExtendedState ^JFrame (p/impl c)) 
                                   JFrame/MAXIMIZED_BOTH))))
    (:size [c _ [w h]]
      (.setSize ^JFrame (p/impl c) w h))
    (:menu [c _ v]
      (.setJMenuBar ^JFrame (p/impl c) (p/impl v))
      (.revalidate ^JFrame (p/impl c)))
    (:icons [c _ v]
      (let [icons (map util/image v)]
        (.setIconImages ^JFrame (p/impl c) icons)))

    ;; Events
    (:on-closed [c _ f]
      (let [listener (proxy [WindowAdapter] []
                       (windowClosed [e] (ui/handle-event f e)))]
        (.addWindowListener ^JFrame (p/impl c) listener)))
    (:on-closing [c _ f]
      (let [listener (proxy [WindowAdapter] []
                       (windowClosing [e] (ui/handle-event f e)))]
        (.addWindowListener ^JFrame (p/impl c) listener)))

    (:on-opened [c _ f]
      (let [listener (proxy [WindowAdapter] []
                       (windowOpened [e] (ui/handle-event f e)))]
        (.addWindowListener ^JFrame (p/impl c) listener)))
    (:on-minimized [c _ f]
      (let [listener (proxy [WindowAdapter] []
                       (windowIconified [e] (ui/handle-event f e)))]
        (.addWindowListener ^JFrame (p/impl c) listener)))
    (:on-restored [c _ f]
      (let [listener (proxy [WindowAdapter] []
                       (windowDeiconified [e] (ui/handle-event f e)))]
        (.addWindowListener ^JFrame (p/impl c) listener))))

(extend-type JFrame
  p/Implementation
  (abstract
    ([this] nil)
    ([this the-abstract] this)))
