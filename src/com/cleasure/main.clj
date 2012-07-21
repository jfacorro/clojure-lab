(ns com.cleasure.main
	(:import
		[javax.swing JFrame JPanel JScrollPane JTextPane JTextArea JTextField JButton JFileChooser UIManager JSplitPane]
		[javax.swing.text StyleContext DefaultStyledDocument]
		[javax.swing.event DocumentListener]
		[java.io OutputStream PrintStream File]
		[java.awt BorderLayout FlowLayout Font]
		[java.awt.event MouseAdapter KeyAdapter KeyEvent])
	(:require 
		[clojure.reflect :as r]
		[com.cleasure.ui.high-lighter :as hl])
	(:use
		[clojure.java.io]))

(def ^:dynamic *app-name* "Cleajure")
(def ^:dynamic *default-font* (Font. "Consolas" Font/PLAIN 14))
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
				methods		(filter #(:return-type %) members)]
			(filter
				#(or (str-contains? % name) (empty name))
				(sort (for [m methods] (:name m)))))))

(defn save-src [txt-code txt-path]
	(let [content (.getText txt-code)]
		(with-open [wrtr (writer (.getText txt-path))]
			(.write wrtr content))))

(defn remove-cr [str] 
	"Removes carriage returns from the string."
	(.replace str "\r" ""))

(defn open-src [txt-code txt-path]
	(let [	dialog	(JFileChooser. *default-dir*)
			result	(. dialog showOpenDialog (. txt-code getParent))
			file	(. dialog getSelectedFile)
			path	(if file (. file getPath) nil)]
		(when path
			(. txt-path setText path)
			(. txt-code setText (remove-cr (slurp path)))
			(hl/high-light txt-code))))

(defn eval-src [txt-code]
	(load-string (.getText txt-code)))

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

(defn set-log [txt]
	(let [out (proxy [OutputStream] []
			(write	([b] (.append txt (str (char b))))
				([b off len] (.append txt (String. b off len)))))]
		(System/setOut (PrintStream. out true))))

(defn make-main [name]
	(let 
		[	main		(JFrame. name)
			txt-code	(JTextPane. (DefaultStyledDocument.))
			pnl-code	(JPanel.)
			txt-log		(JTextArea.)
			txt-path	(JTextField.)
			split		(JSplitPane.)
			pnl-buttons	(JPanel.)
			btn-save		(JButton. "Save")
			btn-open		(JButton. "Open")
			btn-eval		(JButton. "Eval")]

		; Add buttons click handlers
		(on-click btn-open #(open-src txt-code txt-path))
		(on-click btn-save #(save-src txt-code txt-path))
		(on-click btn-eval #(eval-src txt-code))

		; High-light text after key release.
		(on-keyrelease txt-code #(hl/high-light txt-code))

		; Open: CTRL + O
		(on-keypress txt-code #(open-src txt-code txt-path) KeyEvent/VK_O KeyEvent/CTRL_MASK)
		; Save: CTRL + S
		(on-keypress txt-code #(save-src txt-code txt-path) KeyEvent/VK_S KeyEvent/CTRL_MASK)
		; Eval: CTRL + Enter
		(on-keypress txt-code #(println (load-string (.getSelectedText txt-code)))
			KeyEvent/VK_ENTER
			KeyEvent/CTRL_MASK)

		; set-log
		(set-log txt-log)
		
		; Set controls properties
		(.setFont txt-code *default-font*)
		(.setEditable txt-path false)
		(. txt-log setEditable false)

		; buttons panel
		(doto pnl-buttons
			(.setLayout (FlowLayout.))
			(.add btn-save)
			(.add btn-open)
			(.add btn-eval))
		
		(doto pnl-code
			(.setLayout (BorderLayout.))
			(.add txt-code BorderLayout/CENTER))

		(doto split
			(.setOrientation JSplitPane/HORIZONTAL_SPLIT)
			(.setLeftComponent (JScrollPane. pnl-code))
			(.setRightComponent txt-log))

		(doto main
			(.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
			(.setSize 800 600)
			(.add pnl-buttons BorderLayout/NORTH)
			(.add split BorderLayout/CENTER)
			(.add txt-path BorderLayout/SOUTH)
			(.setVisible true))

		(doto split
			(.setDividerLocation 0.8))))

(def main (make-main *app-name*))