(ns com.cleajure.main
	(:import
		[javax.swing JFrame JScrollPane JTextPane JTextField JButton JFileChooser UIManager]
		[javax.swing.text StyleContext DefaultStyledDocument]
		[java.awt BorderLayout Font]
		[java.awt.event MouseAdapter KeyAdapter KeyEvent])
	(:require 
		[clojure.reflect :as r]
		[com.cleasure.ui.high-lighter :as hl])
	(:use
		[clojure.java.io]))

(def ^:dynamic *app-name* "Cleajure")
(def ^:dynamic *default-font* (Font. "Consolas" Font/PLAIN 14))



; Set native look & feel instead of Swings default
(UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName))

(defn str-contains? [s ptrn] 
	"Checks if a string contains a substring"
	(.contains (str s) ptrn))

(defn list-methods 
	([c] (list-methods  c ""))
	([c name]
		(let 
			[members	(:members (r/type-reflect c :ancestors true))
			methods		(filter #(:return-type %) members)]
				(filter
					#(or (str-contains? % name) (empty name))
					(sort (for [m methods] (:name m)))))))

(defn save-src [txt path]
	(let [content (.getText txt)]
		(with-open [wrtr (writer path)]
			(.write wrtr content))))

(defn load-src [txt-code txt-path]
	(let
		[dialog	(JFileChooser.)
		result	(. dialog showOpenDialog (. txt-code getParent))
		file	(. dialog getSelectedFile)
		path	(if file (. file getPath) nil)]
		(when path
			(. txt-path setText path)
			(. txt-code setText (slurp path)))))

(defn eval-src [txt-code]
	(load-string (.getText txt-code))
	(print (.getText txt-code)))

(defn on-click [cmpt f]
	(.addMouseListener cmpt 
		(proxy [MouseAdapter] []
			(mouseClicked [e] (f)))))

(defn on-keypress
	([cmpt f] (on-keypress cmpt f nil nil))
	([cmpt f key mask]
		(.addKeyListener cmpt
			(proxy [KeyAdapter] []
				(keyPressed [e]
					(when (and 
							(or (= key (.getKeyCode e)) (not key))
							(or (= mask (.getModifiers e)) (not mask)))
						(f)))))))

(defn make-main [name]
	(let 
		[main		(JFrame. name)
		txt-code	(JTextPane. (DefaultStyledDocument.))
		txt-path	(JTextField.)
		btn-save	(JButton. "Save")
		btn-load	(JButton. "Load")
		btn-eval	(JButton. "Eval")]
		(.setFont txt-code *default-font*)
		(.setEditable txt-path false)
		(on-click btn-save #(save-src txt-code (.getText txt-path)))
		(on-click btn-load #(load-src txt-code txt-path))
		(on-click btn-eval #(eval-src txt-code))
		(on-keypress txt-code #(hl/high-light txt-code))
		(on-keypress txt-code 
			#(load-string (.getSelectedText txt-code)) 
			KeyEvent/VK_ENTER 
			KeyEvent/CTRL_MASK)
		(doto main
			(.setSize 800 600)
			(.add btn-save BorderLayout/NORTH)
			(.add btn-load BorderLayout/WEST)
			(.add btn-eval BorderLayout/EAST)
			(.add (JScrollPane. txt-code) BorderLayout/CENTER)
			(.add txt-path BorderLayout/SOUTH)
			(.show))))

(def main (make-main *app-name*))