(ns macho.ui.main
  (:use [macho.ui.swing window label font image component text]
        [macho.ui.protocols]))

(def width 500)
(def height 500)
(def app-name "macho")
(def icon-path "./resources/icon-16.png")
(def app-icon "macho")
(def default-font (font {:name "Consolas" :styles [:plain] :size 14}))

(declare 
  ;; Main window.
  main
  ;; Document tabs control
  tabs)

(defn init [title]
  (def main (window title))
  (def lbl (label "Hello, macho!"))
  (def txt (text-pane))

  ;(set-attr! main-win :size [width height])
  (size! main width height)
  ;(set-attr! main :icon (image icon-path))
  (icon! main (image icon-path))
  (font! lbl default-font)
  (font! txt default-font)

  (show! main)
  (add! main lbl)
  (add! main txt))

(init app-name)

(size main)