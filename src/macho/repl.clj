(ns macho.repl
  (:import [javax.swing JFrame JTextArea JScrollPane WindowConstants]
           [java.awt BorderLayout]
           [java.awt.event KeyEvent KeyAdapter WindowAdapter])
  (:require [popen :as p]
            [clojure.string :as string]
            [leiningen.core.eval :as eval]
            [leiningen.core.project :as project]))

(def clojure-repl-cmd ["java" "-cp" "./lib/clojure-1.4.0.jar;./src" "clojure.main"])
  
(defn repl-process
  "Read the project from the specified path, build the
command string to launch a child process and then open the
process."
  [project-path]
  (let [project (project/init-project (project/read project-path))
        cmd     (eval/shell-command project '(do (require 'clojure.main) (clojure.main/main)))]
    (p/popen cmd :redirect true)))

;;----------------------------------------------
;; Create repl UI
;;----------------------------------------------
(defn bind-out-with-txt
  "Binds the output from a stream to the text component."
  [out txt]
  (letfn [(read-out [out txt]
            (while true
              (let [c (.read out)]
                (if (pos? c)
                  (->> c char str (.append txt))))))]
    (doto (Thread. #(read-out out txt))
          (.start))))

(defn write-in
  "Write string to the input stream followed by a newline
and ten flush it."
  [in s]
  (.write in s 0 (count s))
  (.newLine in)
  (.flush in))

(defn check-key 
  "Checks if the key and the modifier match the event's values"
  [evt k m]
  (and 
    (or (nil? k) (= k (.getKeyCode evt)))
    (or (nil? m) (= m (.getModifiers evt)))))

(defn repl-ui [project-path]
  (let [frame    (JFrame. "repl")
        txt-out  (doto (JTextArea.) (.setEditable false))
        txt-in   (doto (JTextArea.) (.setEditable true))
        proc     (repl-process project-path)
        out      (p/stdout proc)
        in       (p/stdin proc)
        thrd-out (bind-out-with-txt out txt-out)]
    (.addKeyListener txt-in
        (proxy [KeyAdapter] []
          (keyPressed [e] 
            (when (check-key e KeyEvent/VK_ENTER KeyEvent/CTRL_MASK)
              (.append txt-out (str (.getText txt-in) "\n"))
              (write-in in (.getText txt-in))
              (.setText txt-in "")))))
    (doto frame
      (.setDefaultCloseOperation WindowConstants/DISPOSE_ON_CLOSE)
      (.add (JScrollPane. txt-out) BorderLayout/CENTER)
      (.add txt-in BorderLayout/SOUTH)
      (.addWindowListener (proxy [WindowAdapter] [] 
                            (windowClosed [e] 
                              (.stop thrd-out)
                              (p/kill proc))))
      (.setVisible true)
      (.setSize 400 400))))

(repl-ui "D:/Juan/Dropbox/dev/Clojure/macanudo/project.clj")

