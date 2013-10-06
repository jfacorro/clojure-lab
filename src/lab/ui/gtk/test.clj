(ns lab.ui.gtk.test
  "Testing an implementation with Gtk+ libraries."
  (:require [lab.ui.core :as ui]
            [lab.ui.protocols :as p]
            [lab.ui.gtk]))

(future
  (let [w (ui/init [:window {:title "Clojure-Lab Dummy" :size [500 500] :icons ["/home/jfacorro/dev/clojure-lab/resources/icon-16.png"]}])]
    (p/show w)
    (org.gnome.gtk.Gtk/main)))
