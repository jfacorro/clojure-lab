(ns lab.ui.swing.window
  (:import  [javax.swing JFrame])
  (:use     [lab.ui.protocols :only [impl]])
  (:require [lab.ui.core :as ui]
            [lab.ui.swing.util :as util]))

(util/definitializations :window JFrame)

(util/defattributes
  :window
    (:menu [c _ v]
      (ui/set-attr c :j-menu-bar (impl v))
      (.revalidate (impl c)))
    (:icons [c _ v]
      (let [icons (map util/image v)]
        (.setIconImages (impl c) icons))))
