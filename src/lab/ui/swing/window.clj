(ns lab.ui.swing.window
  (:import  [javax.swing JFrame])
  (:use     [lab.ui.protocols :only [Implementation impl]])
  (:require [lab.ui.core :as ui]
            [lab.ui.swing.util :as util]))

(ui/definitializations :window JFrame)

(ui/defattributes
  :window
    (:maximized [c _ v]
      (when v
        (.setExtendedState (impl c) (bit-or (.getExtendedState (impl c)) JFrame/MAXIMIZED_BOTH))))
    (:size [c _ [w h]]
      (.setSize (impl c) w h))
    (:menu [c _ v]
      (ui/set-attr c :j-menu-bar (impl v))
      (.revalidate (impl c)))
    (:icons [c _ v]
      (let [icons (map util/image v)]
        (.setIconImages (impl c) icons))))

(extend-type JFrame
  Implementation
  (abstract
    ([this] nil)
    ([this the-abstract] this)))