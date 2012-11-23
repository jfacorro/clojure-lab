(ns macho.core
  (:import [javax.swing JFrame JPanel JScrollPane JTextPane JTextArea 
            JTextField JButton JFileChooser UIManager JSplitPane JTree
            SwingUtilities JTabbedPane JMenuBar JMenu JMenuItem KeyStroke
            JOptionPane]
           [javax.swing.text StyleContext DefaultStyledDocument] 
           [javax.swing.undo UndoManager]
           [javax.swing.event DocumentListener]
           [java.io OutputStream PrintStream File OutputStreamWriter]
           [java.awt BorderLayout FlowLayout Font Color]
           [java.awt.event MouseAdapter KeyAdapter KeyEvent ActionListener]
           [javax.swing.text DefaultHighlighter$DefaultHighlightPainter])
  (:require [clojure.reflect :as r]
            [macho.ui.swing.highlighter :as hl :reload true]
            [macho.ui.swing.undo :as undo :reload true])
  (:use [clojure.java.io]
        [macho.ui.swing image]))
;;------------------------------
(declare main tabs docs tree repl menu)
;;------------------------------
(def app-name "macho")
(def new-doc-title "Untitled")
(def icons-paths ["./resources/icon-16.png" "./resources/icon-32.png" "./resources/icon-64.png"])
(def icons (for [path icons-paths] (image path)))
;;------------------------------
(def ^:dynamic *current-font* (Font. "Consolas" Font/PLAIN 14))
(def default-dir (atom (.getCanonicalPath (File. "."))))
;;------------------------------
;; Set the application look & feel instead of Swings default.
(UIManager/setLookAndFeel "javax.swing.plaf.nimbus.NimbusLookAndFeel")
;; (UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName))
;; (UIManager/setLookAndFeel (UIManager/getCrossPlatformLookAndFeelClassName))
;; (UIManager/setLookAndFeel "com.sun.java.swing.plaf.motif.MotifLookAndFeel")
;;------------------------------
(defn list-methods
  "Lists the methods for the supplied class."
  ([c] (list-methods c ""))
  ([c name]
    (let [members (:members (r/type-reflect c :ancestors true))
          methods (filter #(:return-type %) members)]
      (filter 
        #(or (.contains (str %) name) (empty? name))
        (sort (for [m methods] (:name m)))))))
;;------------------------------
(defn ui-action [f]
  (SwingUtilities/invokeLater f))
;;------------------------------
(defn eval-code 
  "Evaluates the code in the string supplied."
  [^String code]
  (try
    (println (load-string code))
    (catch Exception e
         (println e)
	(println (.getMessage e)))))
;;------------------------------
(defn on-click [cmpt f]
  (.addActionListener cmpt
    (proxy [ActionListener] []
      (actionPerformed [e] (f)))))
;;------------------------------
(defn check-key [evt k m]
  "Checks if the key and the modifier match the event's values"
  (and 
    (or (nil? k) (= k (.getKeyCode evt)))
    (or (nil? m) (= m (.getModifiers evt))))) 
;;------------------------------
(defn on-keypress
  ([cmpt f] (on-keypress cmpt f nil nil))
  ([cmpt f key mask]
    (.addKeyListener cmpt
      (proxy [KeyAdapter] []
        (keyPressed [e] (when (check-key e key mask) (f e)))))))
;;------------------------------
(defn on-keyrelease
  ([cmpt f] (on-keyrelease cmpt f nil nil)) 
  ([cmpt f key mask]
    (.addKeyListener cmpt
      (proxy [KeyAdapter] []
        (keyReleased [e] (when (check-key e key mask) (f e)))))))
;;------------------------------
(defn on-changed [cmpt f]
  (let [doc (.getStyledDocument cmpt)]
    (.addDocumentListener doc
      (proxy [DocumentListener] []
        (changedUpdate [e] nil)
        (insertUpdate [e] (ui-action f))
        (removeUpdate [e] (ui-action f))))))
;;------------------------------
(defn current-txt [tabs]
  (let [idx (.getSelectedIndex tabs)
        scroll (.getComponentAt tabs idx)
        pnl (.. scroll getViewport getView) 
        txt (.getComponent pnl 0)]
  txt))
;;------------------------------
(defn current-path 
  "Finds the current working tab and shows a 
  file chooser window if it's a new file."
  [tabs]
  (let [idx (.getSelectedIndex tabs)
        path (.getTitleAt tabs idx)]
    (if (= path new-doc-title)
      (let [dialog (JFileChooser. @default-dir)
            result (.showSaveDialog dialog nil)
            file (.getSelectedFile dialog)
            path (if file (.getPath file) nil)]
        (when path (.setTitleAt tabs idx path))
        path)
       path)))
;;------------------------------
(defn save-src [tabs]
  (let [txt-code (current-txt tabs)
        path (current-path tabs)
        content (.getText txt-code)]
    (when path
      (spit path content))))
;;------------------------------
(defn eval-src [tabs]
  (let [txt (current-txt tabs)]
    (eval-code (.getText txt))))
;;------------------------------
(defn find-src 
  "Shows the dialog for searching the source
  in the current tabs."
  [tabs]
  (let [txt  (current-txt tabs)
        doc  (.getDocument txt)
        hl   (.getHighlighter txt)
        patr (DefaultHighlighter$DefaultHighlightPainter. Color/LIGHT_GRAY)
        s    (.toLowerCase (hl/remove-cr (.getText txt)))
        ptrn (JOptionPane/showInputDialog tabs "Enter search string:" "Find" JOptionPane/QUESTION_MESSAGE)
        lims (when ptrn (hl/limits (.toLowerCase ptrn) s))]
    (.removeAllHighlights hl)
    (doseq [[a b] lims]
      (.addHighlight hl a b patr))))
;;-------------------------------
(defn update-line-numbers [doc lines]
  (let [pos (.getLength doc)
        root (.getDefaultRootElement doc)
        n (.getElementIndex root pos)]
    (doto lines
      (.setText (apply str (interpose "\n" (range 1 (+ n 2)))))
      (.updateUI))))
;;------------------------------
(defn insert-text
  "Inserts the specified text in the document."
  ([^JTextPane txt s] (insert-text txt s true))
  ([^JTextPane txt s restore]
    (let [doc (.getDocument txt)
          pos (.getCaretPosition txt)]
      (.insertString doc pos s nil)
      (when restore (.setCaretPosition txt pos)))))
;;------------------------------()(898
(defn input-format [^JTextPane txt e]
  "Insert a closing parentesis."
  (let [c (.getKeyChar e)
        k (.getKeyCode e)]
    (cond (= \( c)
            (ui-action #(insert-text txt ")"))
          (= \{ c)
            (ui-action #(insert-text txt "}"))
          (= \[ c)
            (ui-action #(insert-text txt "]"))
          (= KeyEvent/VK_TAB k)
            (do 
              (.consume e)
              (ui-action #(insert-text txt "  " false))))))
;;------------------------------
(defn new-document
  "Adds a new tab to tabs and sets its title."
  ([tabs] (new-document tabs new-doc-title))
  ([tabs title] (new-document tabs title nil))
  ([tabs title src]
    (let [doc (DefaultStyledDocument.)
          txt-code (JTextPane. doc)
          undo-mgr (UndoManager.)
          pnl-code (JPanel.)
          pnl-scroll (JScrollPane. pnl-code)
          txt-lines (JTextArea.)]

      ;; Load the text all at once
      (when src (.setText txt-code src))

      (doto pnl-code
        (.setLayout (BorderLayout.))
        (.add txt-code BorderLayout/CENTER))

      (doto pnl-scroll
         (.setRowHeaderView txt-lines))

      (doto txt-lines
        (.setFont *current-font*)
        (.setEditable false)
        (.setBackground Color/LIGHT_GRAY))

      (update-line-numbers doc txt-lines)

      ;; Eval: CTRL + Enter
      (on-keypress txt-code #(do % (eval-code (.getSelectedText txt-code)))
                   KeyEvent/VK_ENTER KeyEvent/CTRL_MASK)

      ;; Add Undo manager
      (undo/on-undoable doc undo-mgr)

      ;; Undo/redo key events
      (on-keypress txt-code #(do % (when (.canUndo undo-mgr) (.undo undo-mgr)))
                   KeyEvent/VK_Z KeyEvent/CTRL_MASK)
      (on-keypress txt-code #(do % (when (.canRedo undo-mgr) (.redo undo-mgr)))
                   KeyEvent/VK_Y KeyEvent/CTRL_MASK)

      (on-keypress txt-code #(input-format txt-code %))

      ;; High-light text after code edition.
      (on-changed txt-code #(do (hl/high-light txt-code) (update-line-numbers doc txt-lines)))

      (.. pnl-scroll (getVerticalScrollBar) (setUnitIncrement 16))

      (doto tabs
        (.addTab title pnl-scroll)
        (.setSelectedIndex (- (.getTabCount tabs) 1)))

      (doto txt-code
        (.setFont *current-font*)
        (.setBackground Color/black))

      txt-code)))
;;------------------------------
(defn open-src [tabs]
  "Open source file."
  (let [dialog (JFileChooser. @default-dir)
        result (.showOpenDialog dialog nil)
        file (.getSelectedFile dialog)
        path (if file (.getPath file) nil)]
    (when path
      (reset! default-dir (.getCanonicalPath (File. path)))
      (let [src (slurp path)
            txt-code (new-document tabs path src)]
        (doto txt-code
          (.setCaretPosition 0)
          (.grabFocus))
        (hl/high-light txt-code)))))
;;------------------------------
(defn close [tabs]
  "Close the application."
  (let [idx (.getSelectedIndex tabs)]
    (.removeTabAt tabs idx)))
;;------------------------------
(def menu-options
  [{:name "File" 
     :items [
        {:name "New" :action #(print "New") :keys [KeyEvent/VK_N KeyEvent/CTRL_MASK]}
      ]
  }]
)
;;------------------------------
(defn build-menu
  "Builds the application's menu.
  TODO: load from data."
  [tabs txt-repl]
  (let [menubar (JMenuBar.)
        menu-file (JMenu. "File")
        item-new (JMenuItem. "New")
        item-open (JMenuItem. "Open")
        item-save (JMenuItem. "Save")
        item-close (JMenuItem. "Close")
        item-exit (JMenuItem. "Exit")
        menu-code (JMenu. "Code")
        item-eval (JMenuItem. "Eval")
        item-find (JMenuItem. "Find")
        item-clear (JMenuItem. "Clear Log")]

    (on-click item-new #(new-document tabs))
    (.setAccelerator item-new (KeyStroke/getKeyStroke KeyEvent/VK_N KeyEvent/CTRL_MASK))

    (on-click item-open #(open-src tabs))
    (.setAccelerator item-open (KeyStroke/getKeyStroke KeyEvent/VK_O KeyEvent/CTRL_MASK))

    (on-click item-save #(save-src tabs))
    (.setAccelerator item-save (KeyStroke/getKeyStroke KeyEvent/VK_S KeyEvent/CTRL_MASK))

    (on-click item-close #(close tabs))
    (.setAccelerator item-close (KeyStroke/getKeyStroke KeyEvent/VK_W KeyEvent/CTRL_MASK))

    (on-click item-exit #(System/exit 0))
    (.setAccelerator item-exit (KeyStroke/getKeyStroke KeyEvent/VK_X KeyEvent/ALT_MASK))

    (.add menu-file item-new)
    (.add menu-file item-open)
    (.add menu-file item-save)
    (.add menu-file item-close)
    (.add menu-file item-exit)

    (on-click item-eval #(eval-src tabs))
    (.setAccelerator item-eval (KeyStroke/getKeyStroke KeyEvent/VK_E KeyEvent/CTRL_MASK))

    (on-click item-find #(find-src tabs))
    (.setAccelerator item-find (KeyStroke/getKeyStroke KeyEvent/VK_F KeyEvent/CTRL_MASK))

    (on-click item-clear #(.setText txt-repl ""))
    (.setAccelerator item-clear (KeyStroke/getKeyStroke KeyEvent/VK_L KeyEvent/CTRL_MASK))

    (.add menu-code item-eval)
    (.add menu-code item-find)
    (.add menu-code item-clear)

    (.add menubar menu-file)
    (.add menubar menu-code)
    menubar))
;;------------------------------
(defn redirect-out 
  "Creates a PrintStream that writes to the
  JTextArea instance provided and then replaces
  System/out with this stream."
  [^JTextArea txt]
  (let [stream (proxy [OutputStream] []
                 (write
                   ([b off len] (.append txt (String. b off len)))
                   ([b] (.append txt (String. b)))))
        out (PrintStream. stream true)]
    (System/setOut out)
    (System/setErr out)))
;------------------------------------------
;------------------------------------------
(in-ns 'clojure.core)
(use 'clojure.repl)
;------------------------------------------
(defn rebind-out "Allows standard *out* rebinding."
  [out]
  (def ^:dynamic *out-original* *out*)
  (def ^:dynamic *out* out))
;------------------------------------------
;------------------------------------------
(in-ns 'macho.core)
;------------------------------------------
(defn make-main 
  "Creates the main window and all
  its controls."
  [name]
  (let [main (JFrame. name)
        txt-repl (JTextArea.)
        tabs (JTabbedPane.)
        txt-in (JTextArea.)
        pane-repl (JSplitPane.)
        pane-center-left (JSplitPane.)
        pane-all (JSplitPane.)
        files-tree (JTree. (to-array []))]

    ; Redirect std out
    (redirect-out txt-repl)
    (clojure.core/rebind-out (java.io.OutputStreamWriter. System/out))

    ; Set controls properties
    (doto txt-repl
      (.setEditable false)
      (.setFont *current-font*))

    (doto pane-repl
      (.setOrientation JSplitPane/VERTICAL_SPLIT)
      (.setResizeWeight 0.8)
      (.setTopComponent (JScrollPane. txt-repl))
      (.setBottomComponent txt-in))

    (doto pane-center-left
      (.setOrientation JSplitPane/HORIZONTAL_SPLIT)
      (.setResizeWeight 0.8)
      (.setLeftComponent tabs)
      (.setRightComponent pane-repl))

    (doto pane-all
      (.setOrientation JSplitPane/HORIZONTAL_SPLIT)
      (.setLeftComponent (JScrollPane. files-tree))
      (.setRightComponent pane-center-left))

    (.setDividerLocation pane-center-left 0.8)

    (.setDividerLocation pane-all 150)

    (doto main
      (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
      (.setIconImages icons)
      (.setSize 800 600)
      (.setJMenuBar (build-menu tabs txt-repl))
      (.add pane-center-left BorderLayout/CENTER)
      (.setVisible true))))
;;------------------------------
(defn -main
  "Program startup function"
  []
  (make-main app-name))
;;------------------------------
