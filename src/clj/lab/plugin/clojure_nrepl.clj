(ns lab.plugin.clojure-nrepl
  "Clojure REPL process."
  (:require [popen :refer [popen kill stdin stdout]]
            [clojure.java.io :as io]
            [clojure.string :refer [split] :as str]
            [clojure.tools.nrepl :as repl]

            [lab.core :as lab]
            [lab.core [plugin :as plugin]
                      [keymap :as km]]
            [lab.model [document :as doc]
                       [protocols :as model]]
            [lab.ui.core :as ui]
            [lab.ui.templates :as tplts])
   (:import [java.io File BufferedReader]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Namespace symbol list

(def ns-symbols-fns
  '[(defn- ns-aliased-symbols
      "Takes a namespace and returns a set of all the symbols in the aliased namespaces."
      [ns]
      (reduce (fn [s [alias ns]]
                (into s (map (partial str alias "/")
                             (keys (ns-publics ns)))))
              #{}
              (ns-aliases ns)))
    (defn ns-all-symbols
      "Takes a namespace and returns the symbols for all imported classes,
referred vars (without the ns qualifier) and aliased symbols."
      [ns]
      (let [ns (the-ns ns)]
        (->> [(ns-imports ns)
              ;;(ns-interns ns)
              (ns-refers ns)]
             (map (comp (partial map str) keys))
             (reduce into (ns-aliased-symbols ns)))))])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Eval code

(defn eval-in-server
  [{:keys [client] :as conn} code]
  (repl/message client
    {:op   :eval
     :code code}))

(defn response-values
  [responses]
  (->> responses
    (filter :value)
    (map :value)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; nREPL server & client

(defn- locate-file [filename paths]
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

(defn- ensure-dir [file]
  (let [file (io/file file)]
    (if (.isDirectory file)
      file
      (.getParent file))))

(def ^:private lein-cmd "lein")

(def ^:private lein-path
  (or (locate-file lein-cmd (System/getenv "path"))
      (locate-file (str lein-cmd ".bat") (System/getenv "path"))))

(def ^:private nrepl-server-cmd
  [lein-path "repl" ":headless"])

(def ^:private default-nrepl-port nil)

(def ^:private default-host "127.0.0.1")

(defn start-nrepl-server [path]
  (when-not lein-path
    (throw (ex-info "No leiningen command found." {})))
  (let [dir  (io/file (ensure-dir path))
        proc (popen nrepl-server-cmd :dir dir)]
    {:proc proc
     :cin  (stdin proc)
     :cout (stdout proc)}))

(defn stop-nrepl-server [conn]
  (eval-in-server conn "(System/exit 0)"))

(defn start-nrepl-client [path & {:keys [host port]}]
  (let [path      (ensure-dir path)
        port-file (or (locate-file ".nrepl-port" path)
                      (locate-file "target/repl-port" path))
        port      (or (and port (read-string port))
                      (and port-file (read-string (slurp port-file)))
                      default-nrepl-port)
        host      (or host default-host)]
  (when port
    (repl/client (repl/connect :host host :port port) 1000))))

(defn listen-nrepl-server-output!
  "Listen for each line of output from the
server process and pass it to handler."
  [app conn handler]
  (let [cout  ^BufferedReader (get-in conn [:server :cout])]
    (future
      (try 
        (loop [] (handler app conn (.readLine cout)))
        (catch Exception _)))))

(defn handle-nrepl-server-event
  "Takes the app, the connection and a line from the server process
output. Based on the contents of the message, starts an nrepl client
and updates the conn."
  [app {:keys [file id] :as conn} ^String event]
  (cond
    (.contains event "nREPL server started on port")
      (try
        (let [path    (.getCanonicalPath ^File file)
              port    (second (re-find #"port (\d+) " event))
              host    (second (re-find #"host (\d+\.\d+\.\d+\.\d+)" event))
              client  (start-nrepl-client path :port port :host host)]
          (swap! app assoc-in [:connections id :client] client))
          (eval-in-server (get-in @app [:connections id])
                          ns-symbols-fns)
        (catch Exception ex
          (.printStackTrace ex)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; View

(defn- close-tab-repl
  "Ask for confirmation before closing the REPL tab
and killing the associated process."
  [{:keys [source app] :as e}]
  (let [ui   (:ui @app)
        id   (:tab-id (ui/stuff source))
        tab  (ui/find @ui (ui/id= id))
        conn-id (-> (ui/find tab :text-editor) ui/stuff :conn-id)
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

(defn- repl-tab
  "Create the tab that contains the repl and add it
to the ui in the bottom section."
  [app {:keys [id name] :as conn}]
  (let [ui      (:ui @app)
        styles  (:styles @app)
        title   (str "nREPL - " name)]
    (-> (tplts/tab "nrepl")
        (ui/attr :stuff {:close-tab #'close-tab-repl})
        (ui/update-attr :header ui/update :label ui/attr :text title)
        (ui/add [:scroll [:text-editor {:stuff {:conn-id id}}]])
        (ui/apply-stylesheet styles))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Commands

(defn start-and-connect-to-server!
  "Ask the user to select a project file and fire a 
child process with a running nREPL server, then create
an nREPL client that connects to that server."
  [e]
  (let [app           (:app e)
        ui            (:ui @app)
        dir           (lab/config @app :current-dir)
        file-dialog   (ui/init (tplts/open-file-dialog dir @(:ui @app)))
        [result ^File file] (ui/attr file-dialog :result)]
    (when (= result :accept)
      (let [path   (.getCanonicalPath file)
            server (start-nrepl-server path)
            conn-id(gensym "nREPL-")
            conn   {:id     conn-id
                    :server server
                    :file   file
                    :name   (-> file .getParent io/file .getName)}]
        (listen-nrepl-server-output! app conn handle-nrepl-server-event)
        (swap! app assoc-in [:connections conn-id] conn)
        (ui/action 
          (ui/update! ui (ui/parent "bottom")
                         (fn [x]
                           (-> (ui/update-attr x :divider-location-right #(or % 150))
                               (ui/update :#bottom ui/add (repl-tab app conn))))))))))

(defn- connect-to-server!
  [e]
  (throw (ex-info "Not implemented" {})))

(defn- eval-code!
  [{:keys [source app] :as e}]
  (let [ui          (:ui @app)
        editor      source
        file-path   (doc/path @(ui/attr editor :doc))
        [start end] (ui/selection editor)
        selection   (if (= start end)
                      (model/text editor)
                      (model/substring editor start end))
        editor      (ui/find @ui [:#bottom :#nrepl :text-editor])
        conn-id     (:conn-id (ui/stuff editor))]
    (when editor
      (let [responses  (-> (get-in @app [:connections conn-id])
                           (eval-in-server selection))
            values    (response-values responses)]
        (doseq [x values]
          (model/insert editor
                        (model/length editor)
                        (str x "\n")))))))

(defn- init! [app]
  (swap! app assoc :connections {}))

(def ^:private keymaps
  [(km/keymap (ns-name *ns*)
              :global
              {:category "Clojure > nREPL" :name "Start and Connect" :fn ::start-and-connect-to-server! :keystroke "ctrl r"}
              {:category "Clojure > nREPL" :name "Connect" :fn ::connect-to-server!})
   (km/keymap (ns-name *ns*)
              :lang :clojure
              {:category "Clojure > REPL" :name "Eval" :fn ::eval-code! :keystroke "ctrl enter"})])

(plugin/defplugin (ns-name *ns*)
  :type  :global
  :init! #'init!
  :keymaps keymaps)