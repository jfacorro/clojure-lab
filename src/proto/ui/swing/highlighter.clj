(ns proto.ui.swing.highlighter
  (:import [javax.swing.text StyleContext SimpleAttributeSet StyleConstants StyledDocument]
           [javax.swing JTextPane]
           [java.awt Color])
  (:require [proto.lang.clojure :as lang :reload true]
            [proto.ui.swing.core :as util]
            [proto.parser :as parser]
            [proto.ast :as ast]))

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
    (.setCharacterAttributes txt strt end stl true))
  ([^JTextPane txt ^SimpleAttributeSet stl]
    (.setCharacterAttributes txt stl true)))

(defn style
  "Looks for the tag in the syntax map and extracts
  the value for the :style key. If there's no tag element
  then it returns the :style from :default."
  [syntax tag]
  (or (-> syntax tag :style)
      (-> syntax :default :style)))
 
(defn diff-str [a b]
  (apply str (map #(if (= %1 %2) nil (str %1 "<" %2 ">")) a b)))

(defn debug-control-buff-diff [tree txt-pane]
  (println "The buffer's content and the text control content don't match.")
  (spit "text-pane.txt" (.getText txt-pane))
  (spit "buffer.txt" (ast/text tree)))

(defn highlight [^JTextPane txt-pane]
  "Takes the syntax defined by regexes and looks 
  for matches in the text-pane content applying the
  corresponding style to each match."
  (let [doc    (.getDocument txt-pane)
        group  (gensym "node-group-")
        buf    (.getClientProperty txt-pane "buff")
        _      (await buf) ; Wait until all edits thus far have been applied to the buffer
        tree   (parser/parse-tree @buf group)
        limits (ast/get-limits tree group)
        ;; getText from Document returns SO agnostic newlines (\n)
        doctxt (.getText doc 0 (.getLength doc))]
    (when (= (ast/text tree) doctxt)
      (util/queue-action
        (doseq [[strt end tag] limits]
          (apply-style doc strt (- end strt) (style *syntax* tag)))
        (apply-style txt-pane (-> *syntax* :default :style))))))
