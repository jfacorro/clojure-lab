(ns lab.ui.gtk
  (:import [org.gnome.gtk Gtk Window Window$DeleteEvent 
            VBox VPaned
            Label Button Notebook
            TextBuffer TextView
            MenuBar MenuItem Menu])
  (:require [lab.ui [core :as ui]
                    [protocols :as p]]
            [lab.ui.gtk.window]))

(when (not (Gtk/isInitialized))
  (Gtk/init (make-array String 0)))

(ui/defattributes
  :component
    (:id [c _ v])
    (:border [c _ v])
    (:background [c _ v])
    (:color [c _ v])
    (:font [c _ value])
    (:size [c attr [w h :as value]]
      (.setSizeRequest (p/impl c) w h))
    (:on-click [c _ handler])
    (:on-dbl-click [c _ handler]))

(comment

(let [window (Window.)
      menu-bar (MenuBar.)
      file-item(MenuItem. "File")
      file-menu(Menu.)
      box      (VBox. false 3)
      notebk   (Notebook.)
      split    (VPaned.)
      label    (Label. "Bla")
      button   (Button. "Press me!")
      btn2     (Button. "No, press me!")
      buffer   (TextBuffer.)
      text     (TextView. buffer)]
  (.add box menu-bar)
  (.add menu-bar file-item)
  (.setSubmenu file-item file-menu)
  (.add file-menu (MenuItem. "Open"))
  (.add box label)
  (.add box split)
  (.add box text)
  (.add1 split btn2)
  (.add2 split button)
  (.add window box)
  (.setTitle window "Hello!")
  (.showAll window)
  
  (.connect window (proxy [Window$DeleteEvent] []
                     (onDeleteEvent [src, evt] (Gtk/mainQuit) false)))
  (Gtk/main))

)