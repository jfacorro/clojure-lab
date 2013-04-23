(ns macho.repl
  (:require popen
            [leiningen.core.eval :as eval]
            [leiningen.core.project :as project]))

(defrecord Repl [process cin cout])

(def ^:private running-jar 
  "Resolves the path to the current running jar file."
  (-> :keyword class (.. getProtectionDomain getCodeSource getLocation getPath)))

(def ^:private clojure-repl-cmd
  "Builds the command to execute a Clojure repl not 
  associated with any project."
  ["java" "-cp" running-jar "clojure.main"])
  
(defn create
  "Read the leiningen project from the specified path, build the
  command string to launch a child process and then open it."
  ([project-path]
    (let [project (project/init-project (project/read project-path))
          init    '((require 'clojure.main))
          main    (if-let [main (:main project)]
                    `((require '~main) (clojure.main/repl :init #(do (in-ns '~main) (require 'clojure.repl))))
                    `((clojure.main/repl)))
          cmd     (eval/shell-command project (concat '(do) init main))
          proc    (popen/popen cmd :redirect true)]
      (Repl. proc (popen/stdin proc) (popen/stdout proc))))
  ([]
    (let [proc (popen/popen clojure-repl-cmd :redirect true)]
      (Repl. proc (popen/stdin proc) (popen/stdout proc)))))

(defn lab-repl []
  (let [thrd (Thread. clojure.main/repl)]
    (.start thrd)
    (Repl. thrd *out* *in*)))

(defn close [{proc :process :as repl}]
  (cond (instance? Thread proc)
          (.stop proc)
        :else
          (popen/kill proc)))

