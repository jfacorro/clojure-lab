(ns com.cleasure.main
	(:import
		[javax.swing 
			JFrame JPanel JScrollPane JTextPane JTextArea 
			JTextField JButton JFileChooser UIManager JSplitPane 
			SwingUtilities JTabbedPane]
		[javax.swing.text StyleContext DefaultStyledDocument]
		[javax.swing.undo UndoManager]
		[javax.swing.event DocumentListener]
		[java.io OutputStream PrintStream File OutputStreamWriter]
		[java.awt BorderLayout FlowLayout Font]
		[java.awt.event MouseAdapter KeyAdapter KeyEvent])
	(:require 
		[clojure.reflect :as r]
		[com.cleasure.ui.high-lighter :as hl]
		[com.cleasure.ui.text.undo-redo :as undo])
	(:use
		[clojure.java.io]))

(def ^:dynamic *app-name* "Cleajure")
(def ^:dynamic *default-dir* (. (File. ".") getCanonicalPath))

; Set native look & feel instead of Swings default
(UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName))

(defn str-contains? [s ptrn]
	"Checks if a string contains a substring"
	(.contains (str s) ptrn))

(defn list-methods
	([c] (list-methods  c ""))
	([c name]
		(let [	members	(:members (r/type-reflect c :ancestors true))
			methods	(filter #(:return-type %) members)]
			(filter
				#(or (str-contains? % name) (empty name))
				(sort (for [m methods] (:name m)))))))

(defn queue-ui-action [f]
	(SwingUtilities/invokeLater 
		(proxy [Runnable] [] (run [] (f)))))

(defn eval-code [code]
	(println (load-string code)))

(defn on-click [cmpt f]
	(.addMouseListener cmpt 
		(proxy [MouseAdapter] []
			(mouseClicked [e] (f)))))

(defn check-key [evt k m]
	"Checks if the key and the modifier match with the event's values"
	(and 	(or (= k (.getKeyCode evt)) (not k))
			(or (= m (.getModifiers evt)) (not m))))

(defn on-keypress
	([cmpt f] (on-keypress cmpt f nil nil))
	([cmpt f key mask]
		(.addKeyListener cmpt
			(proxy [KeyAdapter] []
				(keyPressed [e] (when (check-key e key mask) (f)))))))

(defn on-keyrelease
	([cmpt f] (on-keyrelease cmpt f nil nil))
	([cmpt f key mask]
		(.addKeyListener cmpt
			(proxy [KeyAdapter] []
				(keyReleased [e] (when (check-key e key mask) (f)))))))

(defn on-changed [cmpt f]
	(let [doc (. cmpt getStyledDocument)]
		(.addDocumentListener doc
			(proxy [DocumentListener] []
				(changedUpdate [e] nil)
				(insertUpdate [e] (queue-ui-action f))
				(removeUpdate [e] (queue-ui-action f))))))

(defn current-txt [tabs]
	(let [	idx		(. tabs getSelectedIndex)
			scroll	(. tabs getComponentAt idx)
			pnl		(.. scroll getViewport getView)
			txt		(. pnl getComponent 0)]
			txt))

(defn current-path [tabs]
	(let [	idx		(. tabs getSelectedIndex)
			path	(. tabs getTitleAt idx)]
			path))

(defn save-src [tabs]
	(let [	txt-code	(current-txt tabs)
			path		(current-path tabs)
			content		(.getText txt-code)]
		(with-open [wrtr (writer path)]
			(.write wrtr content))))

(defn new-document [a b] nil)

(defn open-src [tabs]
	(let [	dialog	(JFileChooser. *default-dir*)
			result	(. dialog showOpenDialog nil)
			file	(. dialog getSelectedFile)
			path	(if file (. file getPath) nil)]
		(when path
			(let [txt-code (new-document tabs path)]
				(. txt-code setText (slurp path))
				(hl/high-light txt-code)))))

(defn eval-src [tabs]
	(let [txt (current-txt tabs)]
		(eval-code (.getText txt))))

(defn new-document [tabs title]
	(let [	doc		(DefaultStyledDocument.)
		txt-code		(JTextPane. doc)
		undo-mgr		(UndoManager.)
		pnl-code		(JPanel.)]

		(doto pnl-code
			(.setLayout (BorderLayout.))
			(.add txt-code BorderLayout/CENTER))

		; Eval: CTRL + Enter
		(on-keypress txt-code #(eval-code (.getSelectedText txt-code))
			KeyEvent/VK_ENTER KeyEvent/CTRL_MASK)

		; Open: CTRL + O
		(on-keypress txt-code #(open-src tabs) KeyEvent/VK_O KeyEvent/CTRL_MASK)
		; Save: CTRL + S
		(on-keypress txt-code #(save-src tabs) KeyEvent/VK_S KeyEvent/CTRL_MASK)

		; Add Undo manager
		(undo/on-undoable doc undo-mgr)

		; Undo/redo key events
		(on-keypress txt-code #(when (.canUndo undo-mgr) (. undo-mgr undo))
			KeyEvent/VK_Z KeyEvent/CTRL_MASK)
		(on-keypress txt-code #(when (.canRedo undo-mgr) (. undo-mgr redo))
			KeyEvent/VK_Y KeyEvent/CTRL_MASK)

		; High-light text after key release.
		(on-changed txt-code #(hl/high-light txt-code))

		(doto tabs
			(.addTab title (JScrollPane. pnl-code))
			(.setSelectedIndex  (- (.getTabCount tabs) 1)))

		txt-code))

(defn redirect-out [txt]
	(let [	stream	(proxy [OutputStream] []
				(write
					([b off len] (. txt append (String. b off len)))
					([b] (. txt append (String. b)))))
		out	(PrintStream. stream true)]
		(System/setOut out)
		(System/setErr out)))

(defn make-main [name]
	(let 
		[	main		(JFrame. name)
			tabs		(JTabbedPane.)
			txt-log		(JTextArea.)
			split		(JSplitPane.)
			pnl-buttons	(JPanel.)
			btn-new		(JButton. "New")
			btn-save		(JButton. "Save")
			btn-open		(JButton. "Open")
			btn-eval		(JButton. "Eval")]

		; Add buttons click handlers
		(on-click btn-new #(new-document tabs "Untitled"))
		(on-click btn-open #(open-src tabs))
		(on-click btn-save #(save-src tabs))
		(on-click btn-eval #(eval-src tabs))

		; Set controls properties
		(. txt-log setEditable false)
		(redirect-out txt-log)

		; buttons panel
		(doto pnl-buttons
			(.setLayout (FlowLayout.))
			(.add btn-new)
			(.add btn-save)
			(.add btn-open)
			(.add btn-eval))

		(doto split
			(.setOrientation JSplitPane/HORIZONTAL_SPLIT)
			(.setLeftComponent tabs)
			(.setRightComponent (JScrollPane. txt-log)))

		(doto main
			;(.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
			(.setSize 800 600)
			(.add pnl-buttons BorderLayout/NORTH)
			(.add split BorderLayout/CENTER)
			(.setVisible true))

		(doto split
			(.setDividerLocation 0.8))))

(def main (make-main *app-name*))

(in-ns 'clojure.core)
(def ^:dynamic *out-custom* (java.io.OutputStreamWriter. System/out))
(def ^:dynamic *out-original* (java.io.OutputStreamWriter. System/out))
(def ^:dynamic *out* *out-custom*)