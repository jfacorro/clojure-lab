(ns macho.repl
  (:require popen
            [leiningen.core.eval :as eval]
            [leiningen.core.project :as project]))

(defrecord Repl [process cin cout])

(def running-jar (-> :bla class (.. getProtectionDomain getCodeSource getLocation getPath)))
(def clojure-repl-cmd ["java" "-cp"running-jar "clojure.main"])
  
(defn create-project-repl
  "Read the leiningen project from the specified path, build the
command string to launch a child process and then open it."
  [project-path]
  (let [project (project/init-project (project/read project-path))
        init    '((require 'clojure.main))
        main    (if-let [main (:main project)]
                  `((require '~main) (clojure.main/repl :init #(in-ns '~main)))
                  `((clojure.main/repl)))
        cmd     (eval/shell-command project (concat '(do) init main))
        proc    (popen/popen cmd :redirect true)]
    (Repl. proc (popen/stdin proc) (popen/stdout proc))))

(defn create-repl
  []
  (let [proc (popen/popen clojure-repl-cmd :redirect true)]
    (Repl. proc (popen/stdin proc) (popen/stdout proc))))

(defn close [repl]
  (popen/kill (:process repl)))

