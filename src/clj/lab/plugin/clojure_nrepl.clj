(ns lab.plugin.clojure-nrepl
  "Clojure nREPL connection.

Most of the ideas for this plugin were taken from the Cider emacs minor mode."
  (:require [popen :refer [popen kill stdin stdout]]
            [clojure.java.io :as io]
            [clojure.string :refer [split] :as str]
            [clojure.tools.nrepl :as repl]
            [lab.core :as lab]
            [lab.core [plugin :as plugin]
                      [keymap :as km]]
            [lab.model [document :as doc]
                       [protocols :as model]
                       [history :as h]]
            [lab.ui.core :as ui]
            [lab.ui.templates :as tplts]
            [lab.plugin.clojure-lang :as clj-lang])
   (:import [java.io File BufferedReader]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Namespace symbol list

(def ^:private ns-symbols-fns
  "Code that is sent to the nREPL server to get all
symbols in *ns*."
  '(letfn [(into! [to from]
            (reduce conj! to from))
           (ns-qualified-public-symbols
             [alias ns]
             (map (partial str alias "/")
                  (keys (ns-publics ns))))
           (ns-aliased-symbols
             [ns]
             (persistent! 
               (reduce (fn [s [alias ns]]
                           (into! s (ns-qualified-public-symbols alias ns)))
                       (transient [])
                       (ns-aliases ns))))
           (ns-symbols-from-map
             [ns]
             (->> ns keys (map str)))
           (ns-all-symbols
             [ns]
             (let [ns (the-ns ns)]
               (persistent!
                 (reduce into!
                       (transient #{})
                       [(ns-symbols-from-map (ns-imports ns))
                        (ns-symbols-from-map (ns-refers ns))
                        (ns-aliased-symbols ns)
                        (mapcat #(ns-qualified-public-symbols (str %) %)
                                (all-ns))]))))]
    (ns-all-symbols *ns*)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Eval code

(defn- eval-in-server
  "Evaluates code by sending it to the server of this connection."
  [{:keys [client current-ns] :as conn} code]
;;  (prn current-ns code)
  (when client
    (repl/message client
      (merge {:op   :eval
              :code code}
             (when current-ns
               {:ns current-ns})))))

(defn- response-values
  [responses]
;;  (prn responses)
  (->> responses
    (filter :value)
    (map :value)))

(defn- response-output
  [responses]
;;  (prn responses)
  (->> responses
    (mapcat #(map (fn [k]
                    (when-let [v (% k)]
                      {:type k :val v}))
                  [:value :out :err]))
    (filter identity)))

(defn- eval-and-get-value [conn code]
  (let [reponses  (eval-in-server conn code)
        [x & _]   (response-values reponses)]
    (and x (read-string x))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; nREPL server & client

(defn- locate-file
  "Takes a filename and a string containing a concatenated
list of directories, looks for the file in each dir and
returns it if found."
  [filename paths]
  (->> (split paths (re-pattern (File/pathSeparator)))
       (map #(as-> (str % (File/separator) filename) path
                   (when (.exists (io/file path)) path)))
       (filter identity)
       first))

(defn- locate-dominating-file [path filename]
  (loop [path path]
    (let [filepath (str path (File/separator) filename)
          file     (io/file filepath)
          parent   (.getParent file)]
      (cond
        (.exists file)
          filepath
        (-> parent nil? not)
          (recur parent)))))

(defn- ensure-dir [path]
  (let [file (io/file path)]
    (if (.isDirectory file)
      path
      (.getParent file))))

(def ^:private lein-cmd "lein")

(def ^:private lein-path
  (or (locate-file lein-cmd (System/getenv "PATH"))
      (locate-file (str lein-cmd ".bat") (System/getenv "PATH"))))

(def ^:private nrepl-server-cmd
  [lein-path "repl" ":headless"])

(def ^:private nrepl-server-cmd-trampoline
  [lein-path "trampoline" "repl" ":headless"])

(def ^:private default-port nil)

(def ^:private default-host "127.0.0.1")

(def ^:private default-ns "user")

(defn- start-nrepl-server [path & [trampoline?]]
  (when-not lein-path
    (throw (ex-info "No leiningen command found." {})))
  (let [dir  (io/file (ensure-dir path))
        proc (popen (if trampoline?
                      nrepl-server-cmd-trampoline
                      nrepl-server-cmd)
                    :dir dir)]
    {:proc proc
     :cin  (stdin proc)
     :cout (stdout proc)}))

(defn- stop-nrepl-server [conn]
  (eval-in-server conn "(System/exit 0)"))

(defn- start-nrepl-client [path & {:keys [host port]}]
  (let [path      (ensure-dir path)
        port-file (or (locate-file ".nrepl-port" path)
                      (locate-file "target/repl-port" path))
        port      (or (and port (read-string port))
                      (and port-file (read-string (slurp port-file)))
                      default-port)
        host      (or host default-host)]
  (when port
    (repl/client (repl/connect :host host :port port) 1000))))

(defn- listen-nrepl-server-output!
  "Listen for each line of output from the
server process and pass it to handler."
  [app conn handler]
  (let [cout ^BufferedReader (get-in conn [:server :cout])]
    (future
      (try 
        (loop [] (handler app conn (.readLine cout)))
        (catch Exception _)))))

(declare console-output!)

(defn- handle-nrepl-server-event
  "Takes the app, the connection and a line from the server process
output. Based on the contents of the message, starts an nrepl client
and updates the conn."
  [app {:keys [file id] :as conn} ^String event]
  (console-output! app (str event "\n"))
  (cond
    (.contains event "nREPL server started on port")
      (try
        (let [path    (.getCanonicalPath ^File file)
              port    (second (re-find #"port (\d+) " event))
              host    (second (re-find #"host (\d+\.\d+\.\d+\.\d+)" event))
              client  (start-nrepl-client path :port port :host host)
              conn    (as-> (assoc conn :client client) conn
                        (assoc conn :current-ns (or (eval-and-get-value conn "(str *ns*)")
                                                    default-ns)))]
          (swap! app assoc-in [:connections id] conn)
          (console-output! app "nREPL client connected\n"))
        (catch Exception ex
          (.printStackTrace ex)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; View

(defn- close-tab
  "Ask for confirmation before closing the REPL tab
and killing the associated process."
  [{:keys [source app] :as e}]
  (let [ui   (:ui @app)
        id   (:tab-id (ui/stuff source))
        tab  (ui/find @ui (ui/id= id))
        conn-id (-> (ui/find tab :split) ui/stuff :conn-id)
        conn (get-in @app [:connections conn-id])
        proc (:server conn)]
    (if (instance? Thread proc)
      (do
        (.stop ^Thread proc)
        (ui/update! ui (ui/parent id) ui/remove tab))
      (let [msg    (str "If you close this tab the nREPL server process"
                        " will be terminated. Do you want to continue?")
            result (tplts/confirm "Closing REPL" msg @ui)]
        (when (= :ok result)
          (stop-nrepl-server conn)
          (swap! app update-in [:connections] dissoc conn-id)
          (ui/update! ui (ui/parent id) ui/remove tab))))))

(defn- console-output!
  "Appends the output to the nREPL console."
  [app output]
  (ui/action
    (ui/update! (:ui @app)
                [:#nrepl :text-editor.output]
                model/append
                output)))

(defn- console-eval-code!
  "Send the code provided to the nREPL server and prints
the results in the output editor."
  [app code]
  (let [ui      (:ui @app)
        console (ui/find @ui [:#nrepl :split])
        conn-id (:conn-id (ui/stuff console))]
    (when conn-id
      (doseq [{:keys [type val]} (-> (get-in @app [:connections conn-id])
                                     (eval-in-server code)
                                     response-output)]
        (console-output! app (if (= type :value) (str val "\n") val))))))

(defn- console-add-history!
  "Adds the code provided to the nREPL console command history."
  [app console-in code]
  (let [id (ui/attr console-in :id)]
    (ui/update! (:ui @app)
                :#nrepl
                ui/update-attr :stuff
                update-in [:history] #(-> %
                                          h/fast-forward
                                          (h/add code)
                                          h/fast-forward))))

(defn- console-eval-input!
  "Evaluates the code that has been entered in the console's 
input editor."
  [{:keys [app source] :as e}]
  (when-let [code  (and (not (empty? (model/text source)))
                        (model/text source))]
    (console-eval-code! app code)
    (console-add-history! app source code)
    (ui/action (ui/attr source :text ""))))

(defn- console-traverse-history!
  [{:keys [app] :as e} dir]
  (ui/update! (:ui @app)
              :#nrepl
              #(let [hist (-> (ui/stuff %) :history dir)]
                (-> (ui/update % :text-editor.input ui/attr :text (h/current hist))
                    (ui/update-attr :stuff assoc :history hist)))))

(defn- console-prev-history!
  [{:keys [app] :as e}]
  (console-traverse-history! e h/rewind))

(defn- console-next-history!
  [{:keys [app] :as e}]
  (console-traverse-history! e h/forward))

(def ^:private console-keymap
  (km/keymap :nrepl :local
             {:keystroke "ctrl enter" :fn ::console-eval-input!}
             {:keystroke "ctrl up" :fn ::console-prev-history!}
             {:keystroke "ctrl down" :fn ::console-next-history!}))

(defn- console-view
  [conn-id]
  [:split {:stuff {:conn-id conn-id}
           :orientation :vertical
           :resize-weight 1
           :divider-location 100}
   [:scroll [:text-editor {:read-only true
                           :class "output"}]]
   [:scroll [:text-editor {:class "input"
                           :listen [:key console-keymap]}]]])

(defn- tab-view
  "Create the tab that contains the repl and add it
to the ui in the bottom section."
  [app {:keys [id name] :as conn}]
  (let [ui      (:ui @app)
        styles  (:styles @app)
        title   (str "nREPL - " name)]
    (-> (tplts/tab "nrepl")
        (ui/attr :stuff {:close-tab #'close-tab
                         :history (h/history)})
        (ui/update-attr :header ui/update :label ui/attr :text title)
        (ui/add (console-view id))
        (ui/apply-stylesheet styles))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Commands

(defn- start-and-connect!
  [{:keys [app] :as e} ^File file & [project?]]
  (let [ui     (:ui @app)
        path   (.getCanonicalPath file)
        conn-id(gensym "nREPL-")
        conn   {:id     conn-id
                :server (start-nrepl-server path project?)
                :file   file
                :name   (-> file .getParent io/file .getName)}
        tab    (-> (tab-view app conn)
                   (ui/update :text-editor.output
                              model/append "Starting nREPL server\n"))]
       (listen-nrepl-server-output! app conn handle-nrepl-server-event)
       (swap! app assoc-in [:connections conn-id] conn)
       (ui/action
         (ui/update! ui (ui/parent "bottom")
                     (fn [x]
                       (-> (ui/update-attr x :divider-location-right #(or % 200))
                           (ui/update :#bottom ui/add tab)))))))
  
  
(defn- start-and-connect-to-repl!
  [{:keys [app] :as e}]
  (start-and-connect! e (io/file "/.")))

(defn- start-and-connect-to-project!
  "Ask the user to select a project file and fire a 
child process with a running nREPL server, then create
an nREPL client that connects to that server."
  [{:keys [app] :as e}]
  (let [ui            (:ui @app)
        dir           (lab/config @app :current-dir)
        file-dialog   (ui/init (tplts/open-file-dialog dir @ui))
        [result file] (ui/attr file-dialog :result)]
    (when (= result :accept)
      (start-and-connect! e file true))))

(defn- connect-to-server!
  [e]
  (throw (ex-info "Not implemented" {})))

(defn- eval-code!
  [{:keys [source app] :as e}]
  (let [[start end] (ui/selection source)
        code        (if (= start end)
                      (model/text source)
                      (model/substring source start end))]
    (console-eval-code! app code)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Autocomplete

(defn- symbols-in-scope-from-connection
  [{:keys [editor app] :as e}]
  (let [ui      (:ui @app)
        console (ui/find @ui [:#nrepl :split])
        conn-id (:conn-id (ui/stuff console))
        conn    (get-in @app [:connections conn-id])]
    (when conn
      (eval-and-get-value conn (str ns-symbols-fns)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Hooks

(defn- switch-document-hook
  [f app doc]
  (if-not doc
    (f app doc)
    (let [app     (f app doc)
          lang    (doc/lang @doc)
          ui      (:ui app)
          console (ui/find @ui [:#nrepl :split])
          conn-id (:conn-id (ui/stuff console))
          conn    (get-in app [:connections conn-id])]
      (if (and conn (= (:name lang) "Clojure"))
        (let [doc-ns (clj-lang/find-namespace @doc :default default-ns)]
          (eval-in-server conn (format "(in-ns '%s)" doc-ns))
          (assoc-in app [:connections conn-id :current-ns] doc-ns))
        app))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Plugin definition

(def ^:private hooks
  {#'lab.core/switch-document #'switch-document-hook})

(def ^:private keymaps
  [(km/keymap (ns-name *ns*)
              :global
              {:category "Clojure > nREPL" :name "Start and Connect to Project" :fn ::start-and-connect-to-project! :keystroke "ctrl r"}
              {:category "Clojure > nREPL" :name "Start and Connect to REPL" :fn ::start-and-connect-to-repl! :keystroke "ctrl alt r"}

              {:category "Clojure > nREPL" :name "Connect" :fn ::connect-to-server!})
   (km/keymap (ns-name *ns*)
              :lang :clojure
              {:category "Clojure > REPL" :name "Eval" :fn ::eval-code! :keystroke "ctrl enter"})])

(defn- init! [app]
  (swap! app assoc :connections {})
  (swap! app
         update-in [:langs :clojure]
         update-in [:autocomplete] conj #'symbols-in-scope-from-connection))

(plugin/defplugin (ns-name *ns*)
  :type    :global
  :init!   #'init!
  :keymaps keymaps
  :hooks   hooks)
