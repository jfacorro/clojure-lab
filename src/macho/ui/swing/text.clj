(ns macho.ui.swing.text
  (:import [javax.swing JTextField JTextPane JTextArea]
           [javax.swing.text DefaultStyledDocument]))

(defn text 
  ([] (text nil))
  ([s] (JTextField. s)))

(defn text-area
  ([] (text-area nil))
  ([s] (JTextArea. s)))

(defn text-pane
  ([] (JTextPane.))
  ([doc] (JTextPane. doc)))

(text-pane)
