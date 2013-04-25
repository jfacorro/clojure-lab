(ns macho.repl
  (:require popen
            [leiningen.core.eval :as eval]
            [leiningen.core.project :as project]
            [clojure.java.io :as io])
  (:import  [java.io PipedOutputStream PipedInputStream]
            [clojure.lang LineNumberingPushbackReader]))

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

(defn lab-repl 
  "Creates a new thread that fires up a thread
  that calls clojure.main/repl, with new bindings
  for *out* an *in*."
  []
  (let [thrd-out  (PipedOutputStream.)
        aux-out   (PipedOutputStream.)
        out       (io/writer thrd-out)
        in        (-> aux-out PipedInputStream. io/reader LineNumberingPushbackReader.)
        cout      (io/writer aux-out)
        cin       (-> thrd-out PipedInputStream. io/reader)
        thrd      (binding [*out* out *in* in *err* out]
                    (Thread. (bound-fn [] 
                               (require 'clojure.main)
                               (clojure.main/repl))))]
    (.start thrd)
    (Repl. thrd cout cin)))

(defn close
  "Terminates a REPL process or thread."
  [{proc :process :as repl}]
  (cond (instance? Thread proc)
          (.stop proc)
        :else
          (popen/kill proc)))

