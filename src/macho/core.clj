(ns macho.core
  (:import [javax.swing JFrame JPanel JScrollPane JTextPane JTextArea 
            JTextField JButton JFileChooser JSplitPane JTree
            JTabbedPane JMenuBar JMenu JMenuItem KeyStroke
            JOptionPane JSeparator]
           [javax.swing.text DefaultStyledDocument DefaultHighlighter$DefaultHighlightPainter]
           [java.io OutputStream PrintStream File OutputStreamWriter]
           [java.awt BorderLayout Font Color]
           [java.awt.event KeyEvent])
  (:require [clojure.reflect :as r]
            [clojure.string :as str]
            [clojure.repl :as repl]
            [clojure.java.io :as io]
            [macho.ui.swing.core :as ui :reload true]
            [macho.ui.swing.highlighter :as hl :reload true]
            [macho.ui.swing.undo :as undo :reload true]
            [macho.ui.swing.text :as txt]
            [macho.ui.protocols :as proto]
            [macho.ui.swing.image :as img]))
;;------------------------------
(declare main tabs docs tree repl menu)
;;------------------------------
(def app-name "macho")
(def new-doc-title "Untitled")
(def icons-paths ["./resources/icon-16.png"
                  "./resources/icon-32.png"
                  "./resources/icon-64.png"])
(def icons (for [path icons-paths] (img/image path)))
;;------------------------------
(def ^:dynamic *current-font* (Font. "Consolas" Font/PLAIN 14))
(def default-dir (atom (.getCanonicalPath (File. "."))))
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
(defn eval-code
  "Evaluates the code in the string supplied."
  [^String code]
  (try
    (println (load-string code))
    (catch Exception e
         (println e)
	(println (.getMessage e)))))
;;------------------------------
(defn check-key
  "Checks if the key and the modifier match the event's values"
  [evt k m]
  (and 
    (or (nil? k) (= k (.getKeyCode evt)))
    (or (nil? m) (= m (.getModifiers evt)))))
;;------------------------------
(defn current-txt [tabs]
  (let [idx (.getSelectedIndex tabs)
        scroll (.getComponentAt tabs idx)
        pnl (.. scroll getViewport getView)
        txt (.getComponent pnl 0)]
  txt))
;;------------------------------
(defn current-doc [tabs]
  (let [txt (current-txt tabs)]
    (.getDocument txt)))
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
        content (proto/text txt-code)]
    (when path
      (spit path content))))
;;------------------------------
(defn eval-src
  "Evaluates source code."
  [tabs]
  (let [txt (current-txt tabs)]
    (eval-code (proto/text txt))))
;;------------------------------
(defn remove-highlight 
  "Removes all highglights from the text control."
  ([txt] (.. txt getHighlighter removeAllHighlights))
  ([txt tag] (.. txt getHighlighter (removeHighlight tag))))
;;------------------------------
(defn add-highlight
  "Add a single highglight in the text control."
  ([txt pos len] (add-highlight txt pos len Color/YELLOW))
  ([txt pos len color]
    (let [doc  (.getDocument txt)
          hl   (.getHighlighter txt)
          pntr (DefaultHighlighter$DefaultHighlightPainter. color)]
      (.addHighlight hl pos (+ len pos) pntr))))
;;------------------------------
(defn find-src 
  "Shows the dialog for searching the source
  in the current tabs."
  [tabs]
  (let [txt  (current-txt tabs)
        s    (str/lower-case (proto/text txt))
        ptrn (JOptionPane/showInputDialog tabs "Enter search string:" "Find" JOptionPane/QUESTION_MESSAGE)
        lims (when ptrn (hl/limits (str/lower-case ptrn) s))]
    (remove-highlight txt)
    (doseq [[a b] lims] (add-highlight txt a (- b a)))))
;;------------------------------
(defn find-doc 
  "Uses the clojure.repl/find-doc function to
search for the selected text in the current docuement."
  [tabs]
  (let [txt  (current-txt tabs)
        slct (.getSelectedText txt)]
    (repl/find-doc slct)))
;;-------------------------------
(defn update-line-numbers [doc lines]
  (let [pos (.getLength doc)
        root (.getDefaultRootElement doc)
        n (.getElementIndex root pos)]
    (ui/queue-action
      #(doto lines
        (.setText (apply str (interpose "\n" (range 1 (+ n 2)))))
        (.updateUI)))))
;;------------------------------
(defn insert-text
  "Inserts the specified text in the document."
  ([^JTextPane txt s] (insert-text txt s true))
  ([^JTextPane txt s restore]
    (let [doc (.getDocument txt)
          pos (.getCaretPosition txt)]
      (.insertString doc pos s nil)
      (when restore (.setCaretPosition txt pos)))))
;;------------------------------
(defn find-char 
  "Finds the next char in s, starting to look from
position cur, in the direction specified by dt (1 or -1)."
  [s cur f dt]

  (cond (or (neg? cur) (<= (.length s) cur)) -1
        (f (nth s cur)) cur
        :else (recur s (+ cur dt) f dt)))
;;------------------------------
(defn insert-tabs 
  "Looks for the first previous \\newline character 
and copies the indenting for the new line."
  [txt]
  (let [pos  (-> (.getCaretPosition txt) dec)
        s    (proto/text txt)
        prev (find-char s pos #(= \newline %) -1)
        end  (find-char s (inc prev) #(not= \space %) 1)
        dt   (dec (- end prev))
        t    (apply str (repeat dt " "))]
    (ui/queue-action #(insert-text txt t false))))
;;------------------------------
(defn input-format 
  "Insert a closing parentesis."
  [^JTextPane txt e]
  (let [c (.getKeyChar e)
        k (.getKeyCode e)]
    (cond (= \( c) (ui/queue-action #(insert-text txt ")"))
          (= \{ c) (ui/queue-action #(insert-text txt "}"))
          (= \[ c) (ui/queue-action #(insert-text txt "]"))
          (= \" c) (ui/queue-action #(insert-text txt "\""))
          (= KeyEvent/VK_ENTER k) (insert-tabs txt)
          (= KeyEvent/VK_TAB k)
            (do (.consume e)
                (ui/queue-action #(insert-text txt "  " false))))))
;;------------------------------
(defn change-font-size [txts e]
  (when (check-key e nil KeyEvent/CTRL_MASK)
    (.consume e)
    (let [font *current-font*
          op   (if (neg? (.getWheelRotation e)) inc #(if (> % 1) (dec %) %))
          size (-> (.getSize font) op)]
      (def ^:dynamic *current-font* (.deriveFont font (float size)))
      (doseq [txt txts] (.setFont txt *current-font*)))))
;;------------------------------
(defn match-paren [s pos end delta]
  "Finds the matching endelimiter for"
  (loop [cur  (+ pos delta) 
         acum 0]
    (cond (neg? cur) nil
          (<= (.length s) cur) nil
          (= (nth s pos) (nth s cur))
            (recur (+ cur delta) (inc acum))
          (= (nth s cur) end) 
            (if (zero? acum) cur 
              (recur (+ cur delta) (dec acum)))
          :else (recur (+ cur delta) acum))))
;;------------------------------
(defn check-paren
  "Checks if the characters in the caret's current
position is a delimiter and looks for the closing/opening
delimiter."
  [txt]
  (let [tags (atom nil)]
    (fn [e]
      (when @tags
        (doseq [tag @tags] (remove-highlight txt tag)))
      (let [pos   (dec (.getDot e))
            s     (proto/text txt)
            c     (get-in s [pos])
            delim {\( {:end \), :d 1}, \) {:end \(, :d -1}
                   \{ {:end \}, :d 1}, \} {:end \{, :d -1}
                   \[ {:end \], :d 1}, \] {:end \[, :d -1}}]
        (when-let [{end :end dir :d} (delim c)]
          (when-let [end (match-paren s pos end dir)]
            (reset! tags 
                    (doall (map #(add-highlight txt % 1 Color/LIGHT_GRAY) [pos end])))))))))
;;------------------------------
(defn new-document
  "Adds a new tab to tabs and sets its title."
  ([tabs]
    (new-document tabs new-doc-title))
  ([tabs title] 
    (new-document tabs title nil))
  ([tabs title src]
    (let [doc (DefaultStyledDocument.)
          txt-code (JTextPane. doc)
          undo-mgr (undo/make-undo-mgr)
          pnl-code (JPanel.)
          pnl-scroll (JScrollPane. pnl-code)
          txt-lines (JTextArea.)]

      (doto pnl-code
        (.setLayout (BorderLayout.))
        (.add txt-code BorderLayout/CENTER))
        
      (doto pnl-scroll
         (.setRowHeaderView txt-lines))

      (doto txt-lines
        (.setFont *current-font*)
        (.setEditable false)
        (.setBackground Color/LIGHT_GRAY))
        
      ;; Eval: CTRL + Enter
      (ui/on :key-press txt-code
             (fn [_] (eval-code (.getSelectedText txt-code)))
               KeyEvent/VK_ENTER KeyEvent/CTRL_MASK)

      ; Undo/redo key events
      (ui/on :key-press txt-code
             (fn [_] (when (.canUndo undo-mgr) (.undo undo-mgr)))
               KeyEvent/VK_Z KeyEvent/CTRL_MASK)
      (ui/on :key-press txt-code
             (fn [_] (when (.canRedo undo-mgr) (.redo undo-mgr)))
               KeyEvent/VK_Y KeyEvent/CTRL_MASK)
               
      (ui/on :key-press txt-code #(input-format txt-code %))

      (ui/on :caret-update txt-code (check-paren txt-code))
      
      ;; Load the text all at once
      (when src (.setText txt-code src))

      ; High-light text after code edition.
      (ui/on :change 
             txt-code
             #(do (hl/high-light txt-code)
                  (update-line-numbers doc txt-lines)))
                  
      ;(ui/on :change txt-code #(println (proto/insertion? %) (proto/text %) %))
                  
      (ui/queue-action #(do (update-line-numbers doc txt-lines) 
                            (hl/high-light txt-code)))
      
      ; Add Undo manager
      (.setLimit undo-mgr -1)
      (ui/on :undoable doc #(undo/handle-edit undo-mgr %))

      ; Set the increment for each vertical scroll
      (.. pnl-scroll (getVerticalScrollBar) (setUnitIncrement 16))

      (ui/on :mouse-wheel pnl-scroll #(change-font-size [txt-code txt-lines] %))

      (doto tabs
        (.addTab title pnl-scroll)
        (.setSelectedIndex (- (.getTabCount tabs) 1)))

      (doto txt-code
        (.setFont *current-font*)
        (.setBackground Color/BLACK)
        (.setCaretPosition 0)
        (.grabFocus))
        
      txt-code)))
;;------------------------------
(defn open-src
  "Open source file."
  [tabs]
  (let [dialog (JFileChooser. @default-dir)
        result (.showOpenDialog dialog nil)
        file (.getSelectedFile dialog)
        path (if file (.getPath file) nil)]
    (when path
      (reset! default-dir (.getCanonicalPath (File. path)))
      (new-document tabs path (slurp path)))))
;;------------------------------
(defn close
  "Close the current tab."
  [tabs]
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
        item-find-doc (JMenuItem. "Find doc")
        item-clear (JMenuItem. "Clear Log")]

    (ui/on :click item-new #(new-document tabs))
    (.setAccelerator item-new (KeyStroke/getKeyStroke KeyEvent/VK_N KeyEvent/CTRL_MASK))

    (ui/on :click item-open #(open-src tabs))
    (.setAccelerator item-open (KeyStroke/getKeyStroke KeyEvent/VK_O KeyEvent/CTRL_MASK))

    (ui/on :click item-save #(save-src tabs))
    (.setAccelerator item-save (KeyStroke/getKeyStroke KeyEvent/VK_S KeyEvent/CTRL_MASK))

    (ui/on :click item-close #(close tabs))
    (.setAccelerator item-close (KeyStroke/getKeyStroke KeyEvent/VK_W KeyEvent/CTRL_MASK))

    (ui/on :click item-exit #(System/exit 0))
    (.setAccelerator item-exit (KeyStroke/getKeyStroke KeyEvent/VK_X KeyEvent/ALT_MASK))

    (.add menu-file item-new)
    (.add menu-file item-open)
    (.add menu-file item-save)
    (.add menu-file item-close)
    (.add menu-file (JSeparator.))
    (.add menu-file item-exit)

    (ui/on :click item-eval #(eval-src tabs))
    (.setAccelerator item-eval (KeyStroke/getKeyStroke KeyEvent/VK_E KeyEvent/CTRL_MASK))

    (ui/on :click item-find #(find-src tabs))
    (.setAccelerator item-find (KeyStroke/getKeyStroke KeyEvent/VK_F KeyEvent/CTRL_MASK))

    (ui/on :click item-find-doc #(find-doc tabs))
    (.setAccelerator item-find-doc (KeyStroke/getKeyStroke KeyEvent/VK_F KeyEvent/ALT_MASK))

    (ui/on :click item-clear #(.setText txt-repl ""))
    (.setAccelerator item-clear (KeyStroke/getKeyStroke KeyEvent/VK_L KeyEvent/CTRL_MASK))

    (.add menu-code item-eval)
    (.add menu-code item-find)
    (.add menu-code item-find-doc)
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
;;------------------------------------------
(defn make-main 
  "Creates the main window and all
its controls."
  [name]
  (let [main (JFrame. name)
        txt-repl (txt/text-area)
        tabs (JTabbedPane.)
        txt-in (txt/text-area)
        pane-repl (JSplitPane.)
        pane-center-left (JSplitPane.)
        pane-all (JSplitPane.)
        files-tree (JTree. (to-array []))]

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

    ; Redirect std out
    (redirect-out txt-repl)
    ; Modify the binding for the *out* var in clojure.core
    (intern 'clojure.core '*out* (java.io.OutputStreamWriter. System/out))

    (doto main
      (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
      (.setIconImages icons)
      (.setSize 800 600)
      (.setJMenuBar (build-menu tabs txt-repl))
      (.add pane-center-left BorderLayout/CENTER)
      (.setVisible true))))
;;------------------------------
(defn -main
  "Program startup function."
  []
  (def main (make-main app-name)))
;;------------------------------
