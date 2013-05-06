(ns macho.ui.swing.core
  (:refer-clojure :exclude [set get])
  (:import  [javax.swing ; Utils
                         UIManager SwingUtilities KeyStroke
                         ; Containers
                         JFrame JPanel JScrollPane JSplitPane JTabbedPane JTree
                         ; Dialogs
                         JFileChooser JOptionPane
                         ; Text
                         JTextField JTextArea JTextPane JLabel JEditorPane
                         text.DefaultStyledDocument text.DefaultHighlighter$DefaultHighlightPainter
                         ; Menu
                         JMenuBar JMenu JSeparator JMenuItem
                         ; Border
                         BorderFactory]
            [java.awt Container Color Toolkit Font BorderLayout AWTEvent Frame]
            [bsh.util JConsole])
  (:require [clojure.string :as str]
            [macho.ui.swing.component]
            [macho.ui.protocols :as proto]
            [macho.misc :as misc]))
;;------------------- 
(def toolkit (Toolkit/getDefaultToolkit))
;;-------------------
(defn init []
  (UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName)))
;;-------------------
;; Expose all vars in macho.ui.protocols
;;-------------------
(macho.misc/intern-vars 'macho.ui.protocols)
(macho.misc/intern-vars 'macho.ui.swing.component)
;;-------------------
(defn queue-action
  "Queues an action to the event queue."
  [f]
  (SwingUtilities/invokeLater f))
;;-------------------
;; Setter & Getters
;;-------------------
(defn- capitalize-word [[x & xs]]
  (apply str (str/upper-case x) xs))
;;-------------------
(defn- capitalize [s]
  (->> (str/split s #"-")      
      (map capitalize-word)
      (apply str)))
;;-------------------
(defn- property-accesor [op prop]
  (symbol (str (name op) (-> prop name capitalize))))
;;-------------------
(defmacro set [obj prop & args]
  `(doto ~obj (.~(property-accesor :set prop) ~@args)))
(defmacro get [obj prop & args]
  `(. ~obj ~(property-accesor :get prop) ~@args))
;;-------------------
(defn color
  ([x] 
    (cond (map? x)
            (let [{:keys [r g b]} x] (color r g b))
          (number? x)
            (color x x x)
          :else
            (throw)))
  ([r g b] (Color. r g b)))
;;-------------------
(defn key-stroke
  "Retrieves the corresponding immutable key stroke object."
  ([k & modif]
    (KeyStroke/getKeyStroke k (apply + modif)))
  ([x]
    (if (instance? AWTEvent x)
      (KeyStroke/getKeyStrokeForEvent x)
      (KeyStroke/getKeyStroke x))))
;;-------------------
;; Container protocol implementation
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
        this))
    (remove-all [this]
      (.removeAll this)))
;;-------------------
;; Frame protocol implementation
;;-------------------
(extend-type java.awt.Frame
  proto/Window
  (maximize [this]
    (.setExtendedState this Frame/MAXIMIZED_BOTH)
    this)
  (minize [this]
    (.setState this Frame/ICONIFIED)
    this)
  proto/Composite
    (add
      ([this child]
        (.add this child)
        (.revalidate this)
        (.repaint this)
        this)
      ([this child args]
        (.add this child args)
        (.revalidate this)
        (.repaint this)
        this))
    (remove-all [this]
      (-> this .getContentPane .removeAll)
      (.revalidate this)
      (.repaint this)
      this))
;;-------------------
(defn frame
  "Creates a new frame."
  [title & {:keys [exit-on-close] :or {exit-on-close true}}]
  (let [frame (JFrame. title)]
    (when exit-on-close
      (.setDefaultCloseOperation frame JFrame/EXIT_ON_CLOSE))
    frame))
;;-------------------
(defn input-dialog [parent title label]
  (JOptionPane/showInputDialog parent label title JOptionPane/QUESTION_MESSAGE))
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
(defn styled-document
  "Creates a styled document."
  []
  (DefaultStyledDocument.))
;;-------------------
(defn editor-pane
  "Creates a new editor pane."
  ([] (JEditorPane.))
  ([doc] (JEditorPane. doc)))
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
    (case orientation
      :vertical
        (doto (JSplitPane.)
          (.setOrientation JSplitPane/VERTICAL_SPLIT)
          (.setTopComponent one)
          (.setBottomComponent two)
          (.setBorder (javax.swing.BorderFactory/createEmptyBorder)))
      :horizontal
        (doto (JSplitPane.)
          (.setOrientation JSplitPane/HORIZONTAL_SPLIT)
          (.setLeftComponent one)
          (.setRightComponent two)
          (.setBorder (javax.swing.BorderFactory/createEmptyBorder))))))
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
  (doto (JTabbedPane.)
    (.setTabLayoutPolicy JTabbedPane/SCROLL_TAB_LAYOUT)
    (.setBorder (javax.swing.BorderFactory/createEmptyBorder))))
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
      (when (= (.showDialog this nil title) JFileChooser/APPROVE_OPTION)
        (.getSelectedFile this)))))
;;-------------------
;; Layouts
;;-------------------
(defn border-layout 
  []
  (BorderLayout.))
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
(defn add-highlight
  "Add a single highglight in the text control."
  ([txt pos len] (add-highlight txt pos len Color/YELLOW))
  ([txt pos len color]
    (let [doc  (.getDocument txt)
          hl   (.getHighlighter txt)
          pntr (DefaultHighlighter$DefaultHighlightPainter. color)]
      (.addHighlight hl pos (+ len pos) pntr))))
;;------------------------------
(defn remove-highlight
  "Removes all highglights from the text control."
  ([txt] (.. txt getHighlighter removeAllHighlights))
  ([txt tag] (.. txt getHighlighter (removeHighlight tag))))
;;-------------------
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
(defn console
  "Creates an instance of a console control."
  [cin cout & close-repl-fn]
  (proxy [JConsole] [cin cout]
    (dispose []
      (when close-repl-fn
        (close-repl-fn)))))
;;-------------------
(defn tree []
  (JTree.))
;;-------------------
(defn border [style]
  (case style
    :empty (BorderFactory/createEmptyBorder)))
;;-------------------
