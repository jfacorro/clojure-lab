(ns lab.ui.gtk.window
  (:import  [org.gnome.gtk Gtk Window Window$DeleteEvent]
            [lab.ui.protocols Implementation])
  (:require [lab.ui.core :as ui]
            [lab.ui.protocols :as p]
            [lab.ui.gtk.util :as util]))

(defn- window-init [c]
  (let [ab (atom c)
        w  (proxy [Window Implementation] []
             (abstract
               ([this] @ab)
               ([this the-abstract] (reset! ab the-abstract) this)))]
    (.connect w (proxy [Window$DeleteEvent] []
                  (onDeleteEvent [src, evt] (Gtk/mainQuit) false)))
    w))

(ui/definitializations :window window-init)

(ui/defattributes
  :window
    (:title [c _ v]
      (.setTitle (p/impl c) v))
    (:menu [c _ v]
      (ui/set-attr c :j-menu-bar (p/impl v))
      (.revalidate (p/impl c)))
    (:icons [c _ v]
      (let [[icon & _] (map util/image v)]
        (.setIcon (p/impl c) icon))))

(extend-protocol p/Visible
  Window
  (show [this]
    (.showAll this)))

(extend-protocol p/Implementation
  Window
  (abstract
    ([this] (.abstract this))
    ([this the-abstract]
      (.abstract this the-abstract)
      this)))
