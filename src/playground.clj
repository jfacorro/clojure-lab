(import 'javax.swing.JTree)
(import 'javax.swing.JSplitPane)
(import 'java.awt.BorderLayout)
(require 'com.cleasure.main)

(def main com.cleasure.main/main)

(def tree (JTree.))

(doto main
	(.add tree BorderLayout/WEST)
	(.setVisible true))