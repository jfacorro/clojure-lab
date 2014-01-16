(ns lab.ui.swing.window
  (:require [lab.ui.core :as ui]
            [lab.ui.swing.util :as util]
            [lab.ui.protocols :as p])
  (:import  [javax.swing JFrame]))

(ui/definitializations :window JFrame)

(ui/defattributes
  :window
    (:background [c _ v]
      (.. (p/impl c) getRootPane (setBackground (util/color v))))
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
        (.setIconImages ^JFrame (p/impl c) icons))))

(extend-type JFrame
  p/Implementation
  (abstract
    ([this] nil)
    ([this the-abstract] this)))
