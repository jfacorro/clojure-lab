(ns macho.ui.swing.highlighter
  (:import [javax.swing.text StyleContext SimpleAttributeSet StyleConstants StyledDocument]
           [javax.swing JTextPane]
           [java.awt Color])
  (:require [macho.lang.clojure :as lang :reload true]
            [macho.ui.swing.core :as util]
            [macho.ast :as ast]))

(def style-constants {:bold StyleConstants/Bold,
                      :background StyleConstants/Background,
	             :foreground StyleConstants/Foreground})

(defn rgb-to-int [rgb]
  "Converts a RGB triple to a single int value."
  (int (+ (* (:r rgb) 65536) (* (:g rgb) 256) (:b rgb))))
 
(defn parse-attrs [stl]
  "Parses the attribute definition, replacing RGB values
  with Color instances."
  (let [color-attr?  #(-> % key #{:foreground :background})
        rgb-to-color (fn [[k v]] [k (Color. (rgb-to-int v))])
        attrs        (->> stl (filter color-attr?) (mapcat rgb-to-color))]
    (apply assoc stl attrs)))

(defn make-style [attrs]
  "Creates a new style with the given
  attributes values."
  (let [style (SimpleAttributeSet.)
        att (parse-attrs attrs)]
    (doseq [[k v] att]
      (.addAttribute style (k style-constants) v))
    style))

(defn init-styles [stls]
  "Initializes the styles for all syntax elements defined."
  (let [f (fn [[k {stl :style}]]
            [k {:style (make-style stl)}])]
    (->> stls (mapcat f) (apply assoc {}))))

(def ^:dynamic *syntax* (init-styles lang/syntax))
(def ^:dynamic *higlighting* (atom false))

(defn apply-style
  "Applies the given style to the text
  enclosed between the strt and end positions."
  ([^StyledDocument txt ^long strt ^long end ^SimpleAttributeSet stl]
    (util/queue-action #(.setCharacterAttributes txt strt end stl true)))
  ([^JTextPane txt ^SimpleAttributeSet stl]
    (util/queue-action #(.setCharacterAttributes txt stl true))))

(defn high-light [^JTextPane txt-pane]
  "Takes the syntax defined by regexes and looks 
  for matches in the text-pane content applying the
  corresponding style to each match."
  (let [doc  (.getDocument txt-pane)
        len  (.getLength doc)
        text (.getText doc 0 len)
        zip  (ast/build-ast text)]
    (doseq [[strt end tag] (ast/get-limits zip)]
      (apply-style doc strt (- end strt)
                   (if (-> *syntax* tag)
                     (-> *syntax* tag :style)
                     (-> *syntax* :default :style))))))
