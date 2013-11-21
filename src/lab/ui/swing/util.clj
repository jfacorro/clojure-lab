(ns lab.ui.swing.util
  (:require [lab.ui.core :as ui]
            [lab.util :as util]
            [lab.ui.protocols :as uip]
            [clojure.java.io :as io])
  (:import [java.awt Color Font Toolkit GraphicsEnvironment]
           [javax.swing BorderFactory JSplitPane KeyStroke ImageIcon]
           [javax.swing.text StyleConstants]))

(def toolkit (Toolkit/getDefaultToolkit))

;; Setter & Getters

(defn setter!
  "Generate a setter interop function for the method whose name
  starts with 'set' followed by prop keyword formatted in camel case
  (e.g. :j-menu-bar turns into JMenuBar). The method takes n arguments
  and the object is type hinted to Class."
  [^Class klass prop n]
  (let [args  (take n (repeatedly gensym))
        hint  (symbol (.getName klass))
        mthd  (util/property-accesor :set prop)
        f     `(fn [^{:tag ~hint} x# ~@args] (. x# ~mthd ~@args))]
    (eval f)))

;; SplitPane Orientations

(def split-orientations
  "Split pane possible orientations."
  {:vertical JSplitPane/VERTICAL_SPLIT :horizontal JSplitPane/HORIZONTAL_SPLIT})

;; Color

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
    
;; Font

(def ^:private font-styles 
  {:bold    Font/BOLD
   :italic  Font/ITALIC
   :plain   Font/PLAIN})

(defn- font-style
  "If style is a vector then a value for the combination of 
  the supplied styles is returned, otherwise a value for the
  single style is generated."
  [style]
  (if (sequential? style)
    (apply bit-or (map #(font-styles % 0) style))
    (font-styles style 0)))

(defn font
  "Takes name, size and style (a vector with :bold and/or :italic)
  all optional and creates a Font."
  ^{:arglists '([string] [:name :size :style])}
  [& [x & xs :as args]]
  (cond (sequential? x)
          (apply font x)
        (string? x)
          (font :name x)
        :else
          (let [{:keys [name size style] :or {size 14 style :plain}} (apply hash-map args)]
            (Font. name (font-style style) size))))

;; Image

(defn image
  "Load an image from a resource file."
  [rsrc]
  (->> rsrc io/resource (.createImage toolkit)))

(defn icon
  "Load an image from a resource file."
  [rsrc]
  (->> rsrc image (ImageIcon.)))

;; Border

(defn border 
  "Takes a style of border and additional arguments according
  to the style:
    :none
    :line color width
    :matte resource
    :titled string"
  [style & [x y & xs]]
  (assert (#{:none :line :matte :titled} style) "Invalid line type.")
  (case style
    :none
      (BorderFactory/createEmptyBorder)
    :line
      (BorderFactory/createLineBorder (color (or x 0)) (or y 1))
    :titled
      (BorderFactory/createTitledBorder x)))

;; KeyStroke

(defn keystroke
  "Returns a swing key stroke based on the string provided."
  [s]
  (KeyStroke/getKeyStroke s))

;; Fullscreen

(def device (-> (GraphicsEnvironment/getLocalGraphicsEnvironment) .getScreenDevices first))

(defn fullscreen
  "Sets the window that will show in fullscreen mode. If the argument
is null, no window is set and the current one (if any) shows fullscreen
no more."
  [window]
  (.setFullScreenWindow device window))
