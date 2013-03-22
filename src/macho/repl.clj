(ns macho.repl
  (:import [javax.swing JFrame JTextArea JScrollPane WindowConstants]
           [java.awt BorderLayout]
           [java.awt.event KeyEvent KeyAdapter WindowAdapter]
           [bsh.util JConsole])
  (:require [popen :as p]
            [leiningen.core.eval :as eval]
            [leiningen.core.project :as project]))

(def clojure-repl-cmd ["java" "-cp" "./lib/clojure-1.4.0.jar;./src" "clojure.main"])
  
(defn repl-process
  "Read the project from the specified path, build the
command string to launch a child process and then open the
process."
  [project-path]
  (let [project (project/init-project (project/read project-path))
        init    '((require 'clojure.main) (clojure.main/main))
        main    (when-let [main (:main project)] 
                  `((require '~main) (in-ns '~main)))
        bla     (println init main (:main project))
        cmd     (eval/shell-command project (concat '(do) main init))]
    (p/popen cmd :redirect true)))

(defn repl-ui
  "Creates a repl session for the leinigen project supplied."
  [project-path]
  (let [process (repl-process project-path)
        cout    (p/stdout process)
        cin     (p/stdin process)
        frame   (JFrame.)
        console (JConsole. cout cin)]
    (doto frame
      (.setDefaultCloseOperation WindowConstants/DISPOSE_ON_CLOSE)
      (.addWindowListener (proxy [WindowAdapter] [] 
                            (windowClosed [e] (p/kill process))))
      (.add console)
      (.setSize 400 400)
      (.setVisible true))))

(repl-ui "./project.clj")


