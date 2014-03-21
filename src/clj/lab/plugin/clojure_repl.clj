(ns lab.plugin.clojure-repl
  "Clojure REPL process."
  (:require popen
            [leiningen.core.eval :as eval]
            [leiningen.core.project :as project]
            [clojure.java.io :as io]
            clojure.main
            [clojure.string :as str]
            [lab.core :as lab]
            [lab.core [plugin :as plugin]
                      [keymap :as km]]
            [lab.ui.core :as ui]
            [lab.ui.templates :as tplts]
            [lab.model.document :as doc]
            [lab.model.protocols :as model])
  (:import  [java.io PipedOutputStream PipedInputStream]
            [clojure.lang LineNumberingPushbackReader]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; REPL process creation

(def ^:private running-jar 
  "Resolves the path to the current running jar file."
  (-> :keyword class (.. getProtectionDomain getCodeSource getLocation getPath)))

(def ^:private clojure-repl-cmd
  "Builds the command to execute a Clojure repl not 
associated with any project."
  ["java" "-cp" running-jar "clojure.main"])

(defn start-repl
  "Read the leiningen project from the specified file, build the
command string to launch a child process and start one with
it. If not project file is supplied, a bare REPL is started."
  ([project-file]
    (let [project (project/init-project (project/read project-file))
          file    (io/file project-file)
          dir     (if (.isDirectory file) file (.getParent file))
          init    '((require 'clojure.main))
          main    (if-let [main (:main project)]
                    `((require '~main) (clojure.main/repl :init #(do (in-ns '~main) (require 'clojure.repl))))
                    `((clojure.main/repl)))
          cmd     (eval/shell-command project (concat '(do) init main))
          proc    (popen/popen cmd :redirect true :dir dir)]
      {:proc proc
       :cin (popen/stdin proc)
       :cout (popen/stdout proc)
       :file project-file
       :name (:name project)}))
  ([]
    (let [proc (popen/popen clojure-repl-cmd :redirect true)]
      {:proc proc :cin (popen/stdin proc) :cout (popen/stdout proc)})))

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
    {:proc thrd
     :cout cin
     :cin  cout}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Eval code

(defn eval-in-console
  "Evaluates the code in the specified repl. Code
  can be a string or a list form.

  args:
    - repl:  the repl where to evaluate the code.
    - code:  string or list with the code to evaluate.
    - echo:  print the code to the repl."
  [console code & {:keys [echo] :or {echo true}}]
  (let [conn  (ui/attr console :conn)
        cin   (:cin conn)]
    (.println (lab.ui.protocols/impl console)
              (when echo code))
    (doto cin
      (.write (str code "\n"))
      (.flush))))

(defn- eval-code!
  [{:keys [source app] :as e}]
  (let [ui          (:ui @app)
        editor      source
        file-path   (doc/path @(ui/attr editor :doc))
        [start end] (ui/selection editor)
        selection   (if (= start end)
                      (if file-path
                        (str "(load-file \"" (str/replace file-path \\ \/) "\")")
                        (model/text editor))
                      (model/substring editor start end))
        console     (ui/find @ui [:#bottom :tab :console])]
    (when console
      (eval-in-console console selection))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; View

(defn- close-tab-repl
  "Ask for confirmation before closing the REPL tab
and killing the associated process."
  [{:keys [source app] :as e}]
  (let [ui   (:ui @app)
        id   (:tab-id (ui/stuff source))
        tab  (ui/find @ui (ui/id= id))
        conn (ui/attr (ui/find tab :console) :conn)
        proc (:proc conn)]
    (if (instance? Thread proc)
      (do
        (.stop ^Thread proc)
        (ui/update! ui (ui/parent id) ui/remove tab))
      (let [result (tplts/confirm "Closing REPL"
                              (str "If you close this tab the REPL process will be killed."
                                   " Do you want to continue?")
                              @ui)]
        (when (= :ok result)
          (popen/kill proc)
          (ui/update! ui (ui/parent id) ui/remove tab))))))

(defn- repl-tab
  "Create the tab that contains the repl and add it
to the ui in the bottom section."
  [app repl]
  (let [ui      (:ui @app)
        styles  (:styles @app)
        title   (str "REPL - "(:name repl))
        tab     (-> (tplts/tab)
                  (ui/attr :stuff {:close-tab #'close-tab-repl})
                  (ui/update-attr :header ui/update :label ui/attr :text title)
                  (ui/add [:scroll [:console {:conn repl}]])
                  (ui/apply-stylesheet styles))
        split   (ui/find @ui (ui/parent "bottom"))
        div-loc (or (ui/attr split :divider-location-right) 0)]
    (when (< div-loc 10)
      (ui/update! ui (ui/parent "bottom") ui/attr :divider-location-right 150))
    (ui/update! ui :#bottom ui/add tab)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Commands

(defn open-project-repl!
  "Ask the user to select a project file and fire a 
child process with a running repl."
  [e]
  (let [app           (:app e)
        dir           (lab/config @app :current-dir)
        file-dialog   (ui/init (tplts/open-file-dialog dir @(:ui @app)))
        [result file] (ui/attr file-dialog :result)]
    (when (= result :accept)
      (let [repl (start-repl (.getCanonicalPath ^java.io.File file))]
        (swap! app update-in [:repls] conj repl)
        (repl-tab app repl)))))

(defn open-repl!
  "Fires up a bare REPL."
  [e]
  (let [app  (:app e)
        repl (start-repl)]
    (swap! app update-in [:repls] conj repl)
    (repl-tab app repl)))

(defn open-lab-repl!
  "Starts a clojure.main/repl in a local thread."
  [e]
  (let [app  (:app e)
        repl (lab-repl)]
    (swap! app update-in [:repls] conj repl)
    (repl-tab app repl)))

(defn- init! [app]
  (swap! app assoc :repls #{}))

(def ^:private keymaps
  [(km/keymap (ns-name *ns*)
              :global
              {:category "Clojure > REPL" :name "Project" :fn ::open-project-repl! :keystroke "ctrl r"}
              {:category "Clojure > REPL" :name "New" :fn ::open-repl! :keystroke "ctrl alt r"}
              {:category "Clojure > REPL" :name "Lab" :fn ::open-lab-repl!})
   (km/keymap (ns-name *ns*)
              :lang :clojure
              {:category "Clojure > REPL" :name "Eval" :fn ::eval-code! :keystroke "ctrl enter"})])

(plugin/defplugin lab.plugin.clojure-repl
  :type  :global
  :init! #'init!
  :keymaps keymaps)
  