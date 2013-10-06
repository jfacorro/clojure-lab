(ns lab.ui.swing.window
  (:import  [javax.swing JFrame])
  (:require [lab.ui.core :as ui]
            [lab.ui.swing.util :as util]
            [lab.ui.protocols :as p]))

(ui/definitializations :window JFrame)

(ui/defattributes
  :window
    (:maximized [c _ v]
      (when v
        (.setExtendedState (p/impl c) 
                           (bit-or (.getExtendedState (p/impl c)) 
                                   JFrame/MAXIMIZED_BOTH))))
    (:size [c _ [w h]]
      (.setSize (p/impl c) w h))
    (:menu [c _ v]
      (ui/set-attr c :j-menu-bar (p/impl v))
      (.revalidate (p/impl c)))
    (:icons [c _ v]
      (let [icons (map util/image v)]
        (.setIconImages (p/impl c) icons))))

(extend-type JFrame
  p/Implementation
  (abstract
    ([this] nil)
    ([this the-abstract] this)))