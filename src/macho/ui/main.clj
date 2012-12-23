(ns macho.ui.main
  (:require [macho.ui.swing [font :as f] 
                            [image :as img]
                            [window :as win]
                            [label :as lbl]
                            [text :as txt]
                            [component :as cmpt]]
            [macho.ui.protocols :as p]))

(def width 500)
(def height 500)
(def app-name "macho - playground")
(def icons-paths ["./resources/icon-16.png" "./resources/icon-32.png"])
(def icons (for [path icons-paths] (img/image path)))
(def app-icon "macho")
(def default-font (f/font :name "Consolas" :styles [:plain] :size 14))

(declare 
  ;; Main window.
  main
  ;; Document tabs control
  tabs)

(println icons)

(defn init [title]
  (def main (win/window title))
  (def lbl (lbl/label "Hello, macho!"))
  (def txt (txt/text-pane))

  (cmpt/size! main width height)
  (win/icons! main icons)
  (cmpt/font! lbl default-font)
  (cmpt/font! txt default-font)

  (p/show! main)
  (p/add! main lbl)
  (p/add! main txt))

(init app-name)

(cmpt/size main)