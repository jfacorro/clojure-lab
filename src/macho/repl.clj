(ns macho.repl
  (:require [popen :as p]
            [leiningen.core.eval :as eval]
            [leiningen.core.project :as project]))

(defrecord Repl [process cin cout])

(def clojure-repl-cmd ["java" "-cp" "." "clojure.main"])
  
(defn create-repl
  "Read the leiningen project from the specified path, build the
command string to launch a child process and then open it."
  [project-path]
  (let [project (project/init-project (project/read project-path))
        init    '((require 'clojure.main))
        main    (when-let [main (:main project)] 
                  `((require '~main) (clojure.main/repl :init #(in-ns '~main))))
        cmd     (eval/shell-command project (concat '(do) init main))
        proc    (p/popen cmd :redirect true)]
    (Repl. proc (p/stdin proc) (p/stdout proc))))

(defn close [repl]
  (p/kill (:process repl)))
