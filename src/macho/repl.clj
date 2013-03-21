(ns macho.repl
  (:import [javax.swing JFrame JTextArea JScrollPane WindowConstants]
           [java.awt BorderLayout]
           [java.awt.event KeyEvent KeyAdapter])
  (:require [popen :as p]
            [clojure.string :as string]
            [leiningen.core.main :as main]
            [leiningen.core.eval :as eval]
            [leiningen.core.project :as project]
            [leiningen.trampoline :as trampoline]))

(def clojure-repl-cmd ["java" "-cp" "./lib/clojure-1.4.0.jar;./src" "clojure.main"])
  
(defn trampoline-cmd-str [project forms profiles]
  ;; each form is (do init & body)
  (let [forms (map rest forms) ;; strip off do
        inits (map first forms)
        rests (mapcat rest forms)
        ;; This won't pick up :jvm-args that come from profiles, but it
        ;; at least gets us :dependencies.
        project (project/set-profiles project profiles)
        command (eval/shell-command project (concat '(do) inits rests))]
  (conj (vec (butlast command)) 
        (with-out-str
          (println (last command))))))

(defn trampoline-task-command-string
  "Runs a project in a new child process and 
returns that process."
  [project task & args]
  (binding [trampoline/*trampoline?* true]
    (main/apply-task (main/lookup-alias task project)
                     (-> (assoc project :eval-in :trampoline)
                         (vary-meta update-in [:without-profiles] assoc
                                    :eval-in :trampoline))
                     args))
  (trampoline-cmd-str
    project 
    @eval/trampoline-forms 
    @eval/trampoline-profiles))

(defn repl-process [project-path]
  (let [project (project/init-project (assoc (project/read project-path) :eval-in :nrepl))
        cmd     (trampoline-task-command-string project "repl")]
    (p/popen cmd :redirect true)))

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
  (let [frame   (JFrame.)
        txt-out (doto (JTextArea.) (.setEditable false))
        txt-in  (doto (JTextArea.) (.setEditable true))
        ;proc    (p/popen clojure-repl-cmd)
        proc    (repl-process project-path)
        out     (p/stdout proc)
        in      (p/stdin proc)]
    (.addKeyListener txt-in
        (proxy [KeyAdapter] []
          (keyPressed [e] 
            (when (check-key e KeyEvent/VK_ENTER KeyEvent/CTRL_MASK)
              (.append txt-out (str (.getText txt-in) "\n"))
              (write-in in (.getText txt-in))
              (.setText txt-in "")))))
    (bind-out-with-txt out txt-out)
    (doto frame
      (.setDefaultCloseOperation WindowConstants/DISPOSE_ON_CLOSE)
      (.add (JScrollPane. txt-out) BorderLayout/CENTER)
      (.add txt-in BorderLayout/SOUTH)
      (.setVisible true)
      (.setSize 400 400))))

(repl-ui "D:/Juan/Dropbox/dev/Clojure/macanudo/project.clj")


