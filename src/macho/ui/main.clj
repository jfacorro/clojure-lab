(ns macho.ui.main
  (:use [macho.ui.swing window label font image component text]
        [macho.ui.protocols]))

(def width 500)
(def height 500)
(def app-name "macho - playground")
(def icons-paths ["./resources/icon-16.png" "./resources/icon-32.png"])
(def icons (for [path icons-paths] (image path)))
(def app-icon "macho")
(def default-font (font {:name "Consolas" :styles [:plain] :size 14}))

(declare 
  ;; Main window.
  main
  ;; Document tabs control
  tabs)

(println icons)

(defn init [title]
  (def main (window title))
  (def lbl (label "Hello, macho!"))
  (def txt (text-pane))

  (size! main width height)
  (icons! main icons)
  (font! lbl default-font)
  (font! txt default-font)

  (show! main)
  (add! main lbl)
  (add! main txt))

(init app-name)

(size main)