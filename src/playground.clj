(import 
  [javax.swing JTree JSplitPane JMenuBar JMenu JMenuItem]
  [java.awt BorderLayout]
  [java.io File])

(require 'clojure.repl)

;------------------------------------------
; Tree
;------------------------------------------
(defn tree-test []
  (def main macho/main)
  (def tree (JTree. (array "one" "two")))
  (doto main
    (.add tree BorderLayout/WEST)
    (.setVisible true)))

(tree-test)
;------------------------------------------
; Menu bar
;------------------------------------------
(defn menu-test []
  (def main macho/main)
  (def menubar (JMenuBar.))
  (def menu (JMenu. "File"))
  (def item (JMenuItem. "Open"))
  (.add menu item)
  (.add menubar menu)
  (.setJMenuBar main menubar))

;------------------------------------------------------------------
(defn load-string
  "Sequentially read and evaluate the set of forms contained in the
  string"
  {:added "1.0"
   :static true}
  [s]
  (let [rdr (-> (java.io.StringReader. s)
                (clojure.lang.LineNumberingPushbackReader.))]
    (clojure.lang.Compiler/load rdr)))

(for [l [\j \u \a \n]] l)

