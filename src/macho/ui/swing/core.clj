(ns macho.ui.swing.core 
  (:refer-clojure :exclude [set get])
  (:import  [javax.swing ; Utils
                         UIManager SwingUtilities
                         ; Containers
                         JFrame JPanel JScrollPane
                         ; Text
                         JTextField JTextArea JTextPane JLabel]
            [java.awt Container Color Toolkit Font])
  (:require [clojure.string :as str]
            [macho.ui.swing.component]
            [macho.ui.swing.util :as util]
            [macho.ui.protocols :as proto]))
;;-------------------
;; Expose Protocols
;;-------------------
(def on proto/on)
(def add proto/add)

(comment 

  (defn intern-vars [from-ns]
    (let [vars (->> from-ns ns-interns (map (comp meta second)))]
      vars))
      
  (-> 'macho.ui.protocols the-ns intern-vars)

)
;;-------------------
(defn queue-action
  "Queues an action to the event queue."
  [f]
  (SwingUtilities/invokeLater f))
;;-------------------
;; Setter & Getters
;;-------------------
(defn capitalize-word [[x & xs]]
  (apply str (str/upper-case x) xs))
(defn capitalize [s]
  (->> (str/split s #"-")      
      (map capitalize-word)
      (apply str)))
;;-------------------
(defn property-accesor [op prop]
  (symbol (str (name op) (-> prop name capitalize))))
;;-------------------
(defmacro set [obj prop & args]
  `(doto ~obj (.~(property-accesor :set prop) ~@args)))
(defmacro get [obj prop & args]
  `(. ~obj ~(property-accesor :get prop) ~@args))
;;-------------------
(defn color 
  ([{r :r g :g b :b}] (color r g b))
  ([r g b] (Color. r g b)))
;;-------------------
;; Component extension
;;-------------------
(extend-type Container
  proto/Visible
    (show [this] (set :visible this true))
    (hide [this] (set :visible this false))
  proto/Composite
    (add [this child] (.add this child) this))
;;-------------------
(defn- init []
  ;Set the application look & feel instead of Swings default.
  (UIManager/setLookAndFeel "javax.swing.plaf.nimbus.NimbusLookAndFeel")
  ;(UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName))
  ;(UIManager/setLookAndFeel (UIManager/getCrossPlatformLookAndFeelClassName))
  ;(UIManager/setLookAndFeel "com.sun.java.swing.plaf.motif.MotifLookAndFeel")
)
;;-------------------
(defn window
  "Creates a new frame."
  [title]
  (JFrame. title))
;;-------------------
(defn text-field
  "Creates a new text field."
  ([] (text-field nil))
  ([s] (JTextField. s)))

(defn text-area
  "Creates a new text area."
  ([] (text-area nil))
  ([s] (JTextArea. s)))

(defn text-pane
  "Creates a new text pane."
  ([] (JTextPane.))
  ([doc] (JTextPane. doc)))
  
(defn label [s]
  "Creates a new label."
  (JLabel. s))

(def toolkit (Toolkit/getDefaultToolkit))
(defn image 
  "Creates a new image."
  [path]
  (.createImage toolkit path))
;;-------------------
;; Font
;;-------------------
(def font-styles {:plain Font/PLAIN
                  :bold Font/BOLD
                  :italic Font/ITALIC})

(defn font
  "Creates a new font using the values from the
following keywords:
  :name   string with font's name.
  :styles sequence that contains a combination of 
          values from the font-style map.
  :size   font size."
  [& {s :name ms :styles n :size}]
  (let [style (reduce bit-and (map font-styles ms))]
    (Font. s style n)))
;;-------------------
(init)
