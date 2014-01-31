(ns lab.plugin.clojure-repl
  "Clojure REPL process."
  (:require popen
            [leiningen.core.eval :as eval]
            [leiningen.core.project :as project]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.core.async :as async]
            [lab.core :as lab]
            [lab.ui.core :as ui]
            [lab.model.document :as doc]
            [lab.model.protocols :as model]
            [lab.ui.templates :as tplts]
            [lab.core [plugin :as plugin]
                      [keymap :as km]]))

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

(defn- eval-in-repl! [app e]
  (let [ui          (:ui @app)
        editor      (:source e)
        file-path   (-> (ui/attr editor :doc) deref doc/path)
        [start end] (ui/selection editor)
        selection   (if (= start end)
                      (if file-path
                        (str "(load-file \"" (.replace file-path \\ \/) "\")")
                        (model/text editor))
                      (model/substring editor start end))
        repl        (ui/find @ui [:#bottom :tab :scroll :text-area])
        in          (-> repl (ui/attr :stuff) :in)]
    (when repl
      (async/put! in selection))))

(defn- wrap-output-stream-in-channel
  "Hook up the output stream of the REPL process
to the channel provided."
  [stream out]
  (async/go
    (loop [input (.read stream)]
      (when (pos? input)
        (async/>! out input)
        (recur (.read stream))))
    (async/close! out)))

(defn- send-to-repl
  "Send code to the input channel of the REPL process
and print the sent code to the REPL console."
  [console cin x]
  (let [x (if (not= (last x) \newline) (str x "\n") x)]
    (ui/action
      (model/insert console (model/length console) x)
      (.write cin (str/replace x #"\n+" "\n"))
      (.flush cin))))

(defn- hook-up-repl
  "Creates two channels that are hooked up to the
output and input streams of the REPL process."
  [repl console]
  (let [in   (async/chan)
        out  (async/chan)
        cout (:cout repl)
        cin  (:cin repl)]
    (wrap-output-stream-in-channel cout out)
    (async/go-loop []
      (when-let [x (async/<! out)]
        (model/insert console (model/length console) (-> x char str))
        (recur)))
    (async/go-loop []
      (when-let [x (async/<! in)]
        (#'send-to-repl console cin x)
        (recur)))
    in))

(defn- close-tab-repl
  "Ask for confirmation before closing the REPL tab
and killing the associated process."
  [app e]
  (let [ui   (:ui @app)
        id   (-> (:source e) (ui/attr :stuff) :tab-id)
        tab  (ui/find @ui (ui/selector# id))
        repl (-> tab
               (ui/find :text-area)
               (ui/attr :stuff)
               :repl)
        result (tplts/confirm "Closing REPL"
                              (str "If you close this tab the REPL process will be killed."
                                   " Do you want to conitnue?"))]
    (when (= :ok result)
      (popen/kill (:proc repl))
      (ui/update! ui (ui/parent id) ui/remove tab))))

(defn- repl-tab
  "Create the tab that contains the repl and add it
to the ui in the bottom section."
  [app repl]
  (let [ui      (:ui @app)
        styles  (:styles @app)
        console (ui/init [:text-area {:read-only true}])
        in      (hook-up-repl repl console)
        console (ui/attr console :stuff {:in in :repl repl})
        tab     (-> (tplts/tab)
                  (ui/update :button ui/attr :on-click ::close-tab-repl)
                  (ui/update :label ui/attr :text (str "REPL: "(:name repl)))
                  (ui/add [:scroll console])
                  (ui/apply-stylesheet styles))
        split   (ui/find @ui (ui/parent "bottom"))
        div-loc (or (ui/attr split :divider-location-right) 0)]
    (when (< div-loc 10)
      (ui/update! ui (ui/parent "bottom") ui/attr :divider-location-right 150))
    (ui/update! ui :#bottom ui/add tab)))

(defn open-project-repl!
  "Ask the user to select a project file and fire a 
child process with a running repl."
  [app e]
  (let [dir           (lab/config @app :current-dir)
        file-dialog   (ui/init (tplts/open-file-dialog dir))
        [result file] (ui/attr file-dialog :result)]
    (when (= result :accept)
      (let [repl (start-repl (.getCanonicalPath file))]
        (swap! app update-in [:repls] conj repl)
        (repl-tab app repl)))))

(defn- init! [app]
  (swap! app assoc :repls #{}))

(def ^:private keymaps
  [(km/keymap (ns-name *ns*)
              :global
              {:category "Clojure > REPL" :name "Connect to project" :fn #'open-project-repl! :keystroke "ctrl r"})
   (km/keymap (ns-name *ns*)
              :lang :clojure
              {:category "Clojure > REPL" :name "Eval" :fn #'eval-in-repl! :keystroke "ctrl enter"})])

(plugin/defplugin lab.plugin.clojure-repl
  :init! #'init!
  :keymaps keymaps)
  