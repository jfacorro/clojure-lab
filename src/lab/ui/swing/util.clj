(ns lab.ui.swing.util
  (:import [java.awt Color Font]
           [javax.swing.text StyleConstants])
  (:require [lab.ui.core :as ui]
            [lab.ui.protocols :as uip]))

(defn- int-to-rgb
  "Converts a single int value int a RGB triple."
  [n]
  (let [r (-> n (bit-and 0xFF0000) (bit-shift-right 16))
        g (-> n (bit-and 0x00FF00) (bit-shift-right 8))
        b (-> n (bit-and 0x0000FF))]
    {:r r :g g :b b}))

(defn rgb-to-int [{:keys [r g b]}]
  "Converts a RGB triple to a single int value."
  (int (+ (* r 65536) (* g 256) b)))

(defn color
  "Takes a map of RGB values (i.e. {:r 0 :g 0 :b 0}), an integer
  representing the three values (0xFFFFFF) or a Color instance,
  and returns a java.awt.Color."
  ([x] 
    (cond (map? x)
            (let [{:keys [r g b]} x]
              (color r g b))
          (integer? x)
            (color (int-to-rgb x))
          (instance? Color x)
            x
          :else
            (throw)))
  ([r g b]
    (Color. r g b)))

(def ^:private font-styles 
  {:bold       StyleConstants/Bold
   :italic     StyleConstants/Italic
   :background StyleConstants/Background
   :foreground StyleConstants/Foreground})

(defn- font-style
  "If style is a vector then a value for the combination of 
  the supplied styles is returned, otherwise a value for the
  single style is generated."
  [style]
  (if (sequential? style)
    (apply bit-and (map #(style % 0) font-styles))
    (font-styles style 0)))

(defn font
  "Takes name, size and style (a vector with :bold and/or :italic)
  all optional and creates a Font."
  ^{:arglists '([string] [:name :size :style])}
  [& [x & xs :as args]]
  (cond (sequential? x)
          (apply font x)
        (string? x)
          (font :name x :size 14 :style :plain)
        :else
          (let [{:keys [name size style]} (apply hash-map args)]
            (Font. name (font-style style) size))))
