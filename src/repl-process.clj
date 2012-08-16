(use 'popen)
(use 'clojure.repl)

(def repl-cmd ["java" "-cp" "../libs/clojure-1.4.0.jar;." "clojure.main"])

(def p (popen repl-cmd :redirect true))

(def out (stdout p))
(def in (stdin p))

(import 'javax.swing.JFrame)
(import 'javax.swing.JTextArea)
(import 'javax.swing.JScrollPane)
(import 'java.awt.BorderLayout)

(def frame (JFrame.))
(def txt (JTextArea.))
(.setEditable txt false)

(doto frame
  (.setVisible true)
  (.setSize 400 400)
  (.add (JScrollPane. txt) BorderLayout/CENTER))

(defn read-out [out txt]
  (while true
    (.append txt (str (char (.read out))))))

(defn write-in [in s]
  (.write in s 0 (count s))
  (.newLine in)
  (.append txt (str s "\n"))
  (.flush in))

(def trh-out (Thread. #(read-out out txt)))
(.start trh-out)

(write-in in "*out*")
(write-in in "(def repl :repl)")
(write-in in "repl")
;(write-in in "(iterate identity 1)")
(write-in in "(ns macho)")