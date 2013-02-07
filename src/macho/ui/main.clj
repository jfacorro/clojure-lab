(remove-ns 'macho.ui.main)
(ns macho.ui.main
  (:require [macho.ui.swing.core :as ui :reload true]))

(def width 500)
(def height 500)
(def app-name "macho - playground")
(def icons-paths ["./resources/icon-16.png" "./resources/icon-32.png"])
(def icons (for [path icons-paths] (ui/image path)))
(def app-icon "macho")
(def default-font (ui/font :name "Consolas" :styles [:plain] :size 14))

(declare 
  ;; Main window.
  main
  ;; Document tabs control
  tabs)

(println icons)

(defn init [title]
  (def main (ui/frame title))
  (def txt (ui/text-pane))
  
  (-> txt
      (ui/set :opaque true)
      (ui/set :background (ui/color 0 0 0))
      (ui/set :font default-font))

  (-> main 
      (ui/set :background (ui/color 0 0 0))
      (ui/set :size width height)
      (ui/set :icon-images icons)
      (ui/add txt)
      (ui/set :visible true)))

(init app-name)
