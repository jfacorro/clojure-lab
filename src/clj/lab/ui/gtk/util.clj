(comment

(ns lab.ui.gtk.util
  (:import [org.gnome.gdk Pixbuf]))

(defn image [filename]
  (Pixbuf. filename))

  )