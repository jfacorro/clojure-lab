(ns lab.plugin.clojure.nrepl
  "Clojure nREPL connection.

  Most of the ideas for this plugin were taken from the [Cider](https://github.com/clojure-emacs/cider)
  and [REPLy](https://github.com/trptcolin/reply)"
  (:require [popen :refer [popen kill stdin stdout]]
            [clojure.java.io :as io]
            [clojure.string :refer [split] :as str]
            [clojure.tools.nrepl :as nrepl]
            [clojure.tools.nrepl.transport :as nrepl.transport]
            [lab.core :as lab]
            [lab.util :as util]
            [lab.core [plugin :as plugin]
                      [keymap :as km]]
            [lab.model [document :as doc]
                       [protocols :as model]
                       [history :as h]]
            [lab.ui.core :as ui]
            [lab.ui.templates :as tplts]
            [lab.plugin.clojure.lang :as clj-lang])
   (:import [java.io File BufferedReader]
            [java.util.concurrent LinkedBlockingQueue TimeUnit]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Namespace symbol list

(def ^:private ns-symbols-fns
  "Code that is sent to the nREPL server to get all symbols in *ns*."
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

(declare console-output!)

(defn- poll-responses
  [app {:keys [queue connection] :as conn}]
  (let [poll (fn []
               (let [continue? (try
                                 (when-let [{:keys [out err] :as response} (nrepl.transport/recv connection)]
                                   (when err (console-output! app err))
                                   (when out (console-output! app out))
                                   (when-not (or err out)
                                     (.offer ^LinkedBlockingQueue queue response)))
                                 true
                                 (catch Exception _ false))]
                 (when continue?
                   (recur))))]
    (doto (Thread. poll)
      (.setName "nREPL poller")
      (.setDaemon true)
      (.start))))

(defn- responses
  [{:keys [queue] :as conn}]
  (when queue
    (lazy-seq
      (cons (.poll ^LinkedBlockingQueue queue 50 TimeUnit/MILLISECONDS)
        (responses conn)))))

(defn- done? [response]
  (some #{"done" "error"} (:status response)))

(defn- command-responses
  [conn]
  (->> (responses conn)
    (filter identity)
    (take-while (comp not done?))))

(defn- eval-in-server
  "Evaluates code by sending it to the server of this connection."
  [{:keys [client current-ns] :as conn} code]
  (when client
    (let [message (merge {:op :eval, :code code} 
                         (and current-ns {:ns current-ns}))]
      (nrepl/message client message))))

(defn- eval-and-get-value
  [conn code]
  (eval-in-server conn code)
  (first (nrepl/response-values (command-responses conn))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; nREPL server & client

(def ^:private lein-cmd "lein")
(def ^:private lein-path
  (or (util/locate-file lein-cmd (System/getenv "PATH"))
      (util/locate-file (str lein-cmd ".bat") (System/getenv "PATH"))))

(def ^:private nrepl-server-cmd [lein-path "repl" ":headless"])
(def ^:private nrepl-server-cmd-trampoline [lein-path "trampoline" "repl" ":headless"])

(def ^:private default-port nil)
(def ^:private default-host "127.0.0.1")
(def ^:private default-ns "user")

(defn- start-nrepl-server [path & [trampoline?]]
  (when-not lein-path
    (throw (ex-info "No leiningen command found in PATH." {})))
  (let [dir  (io/file (util/ensure-dir path))
        proc (popen (if trampoline?
                      nrepl-server-cmd-trampoline
                      nrepl-server-cmd)
                    :dir dir)]
    {:proc proc
     :cin  (stdin proc)
     :cout (stdout proc)}))

(defn- stop-nrepl-server
  [{:keys [connection] :as conn}]
  (eval-in-server conn "(System/exit 0)")
  (when (isa? (class connection) java.io.Closeable)
    (.close ^java.io.Closeable connection)))

(defn- create-nrepl-client
  "Creates an nREPL client, returning a map with its connection, the client
  function and a queue that holds the responses."
  [& {:keys [host port]}]
  (let [port       (or (and port (read-string port))
                       default-port)
        host       (or host default-host)
        connection (nrepl/connect :host host :port port)]
    {:connection connection
     :client     (nrepl/client connection Long/MAX_VALUE)
     :queue      (LinkedBlockingQueue.)}))

(defn- listen-nrepl-server-output!
  "Listen for each line of output from the server process and 
  pass it to handler."
  [app conn handler]
  (let [cout ^BufferedReader (get-in conn [:server :cout])]
    (future
      (try
        (loop [event (.readLine cout)]
          (when event
            (handler app conn event)
            (recur (.readLine cout))))
        (catch Exception ex)))))

(defn- handle-nrepl-server-event
  "Takes the app, the connection and a line from the server process
  output. Based on the contents of the message, starts an nrepl client
  and updates the conn."
  [app {:keys [id] :as conn} ^String event]
  (console-output! app (str event "\n"))
  (when (and event (re-find #"nREPL server started on port" event))
    (try
      (let [port        (second (re-find #"port (\d+) " event))
            host        (second (re-find #"host (\d+\.\d+\.\d+\.\d+)" event))
            client-info (create-nrepl-client :host host :port port)
            conn        (merge conn client-info) 
            _           (poll-responses app conn)
            conn        (assoc conn :current-ns (or (eval-and-get-value conn "(str *ns*)")
                                                    default-ns))]
        (swap! app assoc-in [:connections id] conn)
        (console-output! app "nREPL client connected\n"))
      (catch Exception ex))))

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
        conn-id (:conn-id (ui/stuff console))
        conn    (get-in @app [:connections conn-id])]
    (when conn-id
      (eval-in-server conn code)
      (future 
        (doseq [{:keys [ns value] :as res} (command-responses conn)]
          (when value
            (console-output! app (str value "\n"))))))))

(defn- console-add-history!
  "Adds the code provided to the nREPL console command history."
  [app console-in code]
  (ui/update! (:ui @app)
              :#nrepl
              ui/update-attr :stuff
              update-in [:history] #(-> %
                                        h/fast-forward
                                        (h/add code)
                                        h/fast-forward)))

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
  (km/keymap "nREPL Console"
    :local
    {:keystroke "ctrl enter" :fn ::console-eval-input! :name "Eval"}
    {:keystroke "ctrl up" :fn ::console-prev-history! :name "Prev History"}
    {:keystroke "ctrl down" :fn ::console-next-history! :name "Next History"}))

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
        tab    (tab-view app conn)]
    (listen-nrepl-server-output! app conn handle-nrepl-server-event)
    (swap! app assoc-in [:connections conn-id] conn)
    (ui/action
      (ui/update! ui (ui/parent "bottom")
        (fn [x]
          (-> (ui/update-attr x :divider-location-right #(or % 200))
            (ui/update :#bottom ui/add tab))))
      (console-output! app "Starting nREPL server\n"))))
  
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
          lang    (:lang @doc)
          ui      (:ui app)
          console (ui/find @ui [:#nrepl :split])
          conn-id (:conn-id (ui/stuff console))
          conn    (get-in app [:connections conn-id])]
      (if (and conn (= (:name lang) "Clojure"))
        (let [doc-ns (clj-lang/find-namespace @doc :default default-ns)]
          (eval-and-get-value conn (format "(ns %s)" doc-ns))
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
              {:category "Clojure > nREPL" :name "Eval" :fn ::eval-code! :keystroke "ctrl enter"})])

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
