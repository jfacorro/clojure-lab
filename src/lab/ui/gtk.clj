(ns lab.ui.gtk)
(require '[cemerick.pomegranate :as pom])
(pom/add-classpath "/usr/share/java/gtk.jar")
(pom/add-classpath "/usr/share/java/gtk-4.1.jar")

(ns lab.ui.gtk
  (:import [org.gnome.gtk Gtk Window Window$DeleteEvent 
            VBox VPaned
            Label Button Notebook
            TextBuffer TextView
            MenuBar MenuItem Menu])
  (:require [lab.ui.core :as ui]
            [lab.ui.protocols :as p]
            [lab.ui.gtk.window :reload true]))

(when (not (Gtk/isInitialized))
  (Gtk/init (make-array String 0)))

(ui/defattributes
  :component
    (:border [c _ v])
    (:background [c _ v])
    (:foreground [c _ v])
    (:font [c _ value])
    (:size [c attr [w h :as value]]
      (.setSizeRequest (p/impl c) w h))
    (:on-click [c _ handler])
    (:on-dbl-click [c _ handler]))


#_(let [window (Window.)
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

(comment
  
  l = new Label("Go ahead:\nMake my day");
  x.add(l);
  w.setTitle("Hello World");
  w.showAll();
)