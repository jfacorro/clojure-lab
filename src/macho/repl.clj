(ns macho.repl
  (:require popen
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
        proc    (popen/popen cmd :redirect true)]
    (Repl. proc (popen/stdin proc) (popen/stdout proc))))

(defn close [repl]
  (popen/kill (:process repl)))
