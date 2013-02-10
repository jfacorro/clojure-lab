(remove-ns 'macho.ui.swing.core)
(ns macho.ui.swing.core
  (:refer-clojure :exclude [set get])
  (:import  [javax.swing ; Utils
                         UIManager SwingUtilities
                         ; Containers
                         JFrame JPanel JScrollPane JSplitPane JTabbedPane
                         ; Dialogs
                         JFileChooser
                         ; Text
                         JTextField JTextArea JTextPane JLabel
                         ; Menu
                         JMenuBar JMenu JSeparator JMenuItem]
            [java.awt Container Color Toolkit Font])
  (:require [clojure.string :as str]
            [macho.ui.swing.component]
            [macho.ui.protocols :as proto]
            [macho.misc :as misc]))
;;------------------- 
(def toolkit (Toolkit/getDefaultToolkit))
;;-------------------
;; Expose intern all Vars 
;; in macho.ui.protocols
;;-------------------
(macho.misc/intern-vars 'macho.ui.protocols *ns*)
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
(extend-type java.awt.Container
  proto/Visible
    (show [this] (set this :visible true) this)
    (hide [this] (set this :visible false) this)
  proto/Composite
    (add
      ([this child]
        (.add this child)
        this)
      ([this child args]
        (.add this child args)
        this)))
;;-------------------
(defn frame
  "Creates a new frame."
  [title]
  (JFrame. title))
;;-------------------
(defn text-field
  "Creates a new text field."
  ([] (text-field nil))
  ([s] (JTextField. s)))
;;-------------------
(defn text-area
  "Creates a new text area."
  ([] (text-area nil))
  ([s] (JTextArea. s)))
;;-------------------
(defn text-pane
  "Creates a new text pane."
  ([] (JTextPane.))
  ([doc] (JTextPane. doc)))
;;-------------------
(defn label [s]
  "Creates a new label."
  (JLabel. s))
;;-------------------
(defn image 
  "Creates a new image."
  [path]
  (.createImage toolkit path))
;;-------------------
(defn split
  "Creates a new split container."
  ([one two] 
    (split one two :horizontal))
  ([one two orientation]
    (case 
      :vertical
        (doto (JSplitPane.)
          (.setOrientation JSplitPane/VERTICAL_SPLIT)
          (.setTopComponent one)
          (.setBottomComponent two))
      :horizontal
        (doto (JSplitPane.)
          (.setOrientation JSplitPane/HORIZONTAL_SPLIT)
          (.setLeftComponent one)
          (.setRightComponent two)))))
;;-------------------
(defn scroll
  "Wraps the control in a scrolling pane."
  [ctrl]
  (JScrollPane. ctrl))
;;-------------------
(defn panel
  "Creates a panel container."
  []
  (JPanel.))
;;-------------------
(defn tabbed-pane
  "Creates a tabbed container."
  []
  (JTabbedPane.))
;;-------------------
(defn file-browser 
  "Show a dialog box that allows to browse the file
system and select a file."
  ([] (JFileChooser.))
  ([default-dir] (JFileChooser. default-dir)))
;;-------------------
(extend-type JFileChooser
  proto/Visible
  (show 
    ([this]
      (show this ""))
    ([this title]
      (.showDialog this nil title)
      (.getSelectedFile this))))
;;-------------------
;; Menu
;;-------------------
(defn menu-separator
  []
  (JSeparator.))
;;-------------------
(defn menu-item
  [name]
  (JMenuItem. name))
;;-------------------
(defn menu-bar
  []
  (JMenuBar.))
;;-------------------
(defn menu
  [name]
  (JMenu. name))
;;-------------------
;; Font
;;-------------------
(def font-styles {:plain Font/PLAIN
                  :bold Font/BOLD
                  :italic Font/ITALIC})
;;-------------------
(defn font
  "Creates a new font using the values from the
following keywords:
  :name   string with font's name.
  :styles sequence that contains a combination of 
          values from the font-style map.
  :size   font size."
  [& {s :name ms :styles n :size color :color}]
  (let [style (reduce bit-and (map font-styles ms))]
    (Font. s style n)))
;;-------------------
(defn- init []
  ;Set the application look & feel instead of Swings default.
  ;(UIManager/setLookAndFeel "javax.swing.plaf.nimbus.NimbusLookAndFeel")
  (UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName))
  ;(UIManager/setLookAndFeel (UIManager/getCrossPlatformLookAndFeelClassName))
  ;(UIManager/setLookAndFeel "com.sun.java.swing.plaf.motif.MotifLookAndFeel")
)
;;-------------------
(init)
