(ns macho.ui.swing.image
  (:import [java.awt Image Toolkit]))

(def toolkit (Toolkit/getDefaultToolkit))

(defn image [path]
  (.createImage toolkit path))