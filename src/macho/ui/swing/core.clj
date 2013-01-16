(ns macho.ui.swing.core 
  (:refer-clojure :exclude [set get])
  (:import  [javax.swing UIManager JFrame 
                         ;Text controls
                         JTextField JTextArea JTextPane JLabel]
            [java.awt Container Color])
  (:require [clojure.string :as str]
            [macho.ui.swing.component]
            [macho.ui.swing.util :as util]
            [macho.ui.protocols :as proto]))
;;-------------------
;; Expose API
;;-------------------
(def queue-action util/queue-action)
(def on proto/on)
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
;;-------------------
(init)
