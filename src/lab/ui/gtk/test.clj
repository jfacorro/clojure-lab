(ns lab.ui.gtk.test
  "DSL to abstract the UIcomponents with Clojure data structures."
  (:require [lab.ui [core :as ui :reload true]
                    [protocols :as p :reload true]
                    [gtk :reload true]]))

(let [w (ui/init [:window {:title "Clojure-Lab Dummy" :size [500 500] :icons ["/home/jfacorro/sd-card/dev/clojure-lab/resources/icon-16.png"]}])]
  (p/show w)
  (org.gnome.gtk.Gtk/main))