(ns lab.ui.swing.window
  (:import  [javax.swing JFrame])
  (:require [lab.ui.core :as ui]
            [lab.ui.swing.util :as util]
            [lab.ui.protocols :as p]))

(ui/definitializations :window JFrame)

(ui/defattributes
  :window
    (:title [c _ v]
      (.setTitle (p/impl c) v)
      c)
    (:maximized [c _ v]
      (when v
        (.setExtendedState ^JFrame (p/impl c) 
                           (bit-or (.getExtendedState ^JFrame (p/impl c)) 
                                   JFrame/MAXIMIZED_BOTH))))
    (:size [c _ [w h]]
      (.setSize ^JFrame (p/impl c) w h))
    (:menu [c _ v]
      (.setJMenuBar (p/impl c) (p/impl v))
      (.revalidate ^JFrame (p/impl c)))
    (:icons [c _ v]
      (let [icons (map util/image v)]
        (.setIconImages ^JFrame (p/impl c) icons))))

(extend-type JFrame
  p/Implementation
  (abstract
    ([this] nil)
    ([this the-abstract] this)))