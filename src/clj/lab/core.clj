(ns lab.core
  (:refer-clojure :exclude [name])
  (:require [lab.model.document :as doc]
            [lab.core.keymap :as km]
            [lab.core.plugin :as pl]
            [lab.core.lang   :as lang]
            [clojure.java.io :as io])
  (:import [java.io File]))

(declare 
  current-document
  open-document 
  save-document
  close-document
  config)

(def default-config
  {:name          "Clojure Lab"
   :core-plugins  '[lab.core.main]
   :plugins       '[; Editor plugins
                    lab.plugin.editor.go-to
                    lab.plugin.editor.undo-redo

                    ; Main plugins 
                    lab.plugin.notifier
                    lab.plugin.file-explorer
                    lab.plugin.find-replace
                    lab.plugin.code-outline
                    lab.plugin.helper

                    ; Languages
                    lab.plugin.markdown.lang
                    lab.plugin.clojure.lang

                    ;Clojure plugins
                    lab.plugin.clojure.nrepl]
   :lang-plugins  '{:clojure [lab.plugin.editor.syntax-highlighting
                              lab.plugin.editor.autocomplete
                              lab.plugin.editor.delimiter-matching
                              lab.plugin.editor.rainbow-delimiters
                              lab.plugin.editor.paredit]
                    :markdown [lab.plugin.editor.syntax-highlighting]}
   :plugins-dir   "plugins"
   :current-dir   "."
   :default-lang  (:id lang/plain-text)})

(def default-app
  "Returns a new app with nothing initialized and a
default configuration."
  {:name              "Clojure Lab"
   :config            default-config
   :documents         #{}
   :current-document  nil
   :langs             {(:id lang/plain-text) lang/plain-text}
   :keymap            nil})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Language

(defn default-lang
  "Returns the default language."
  [{:keys [langs config] :as app}]
  (langs (config :default-lang)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Keymap registration

(defmethod km/register-multi :global
  [app keymap]
  (update-in app [:keymap] km/append keymap))

(defmethod km/register-multi :lang
  [app {lang :lang :as keymap}]
  (update-in app [:langs lang :keymap] km/append keymap))

(defmethod km/register-multi :local
  [app keymap]
  (when-let [doc (current-document app)]
    (swap! doc update-in [:keymap] km/append keymap))
  app)

(defmethod km/unregister-multi :global
  [app keymap]
  (update-in app [:keymap] km/remove (:name keymap)))

(defmethod km/unregister-multi :lang
  [app {lang :lang :as keymap}]
  (update-in app [:langs lang :keymap] km/remove (:name keymap)))

(defmethod km/unregister-multi :local
  [app keymap]
  (when-let [doc (current-document app)]
    (swap! doc update-in [:keymap] km/remove (:name keymap)))
  app)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Plugin registration

(defn- conj-set [s plugin]
  (conj (or s #{}) plugin))

(defmethod pl/register-plugin! :global
  [app plugin]
  (when-not (contains? (:plugins @app) plugin)
    (swap! app update-in [:plugins] conj-set plugin)))

(defmethod pl/register-plugin! :local
  [app plugin]
  (when-let [doc (current-document @app)]
    (when-not (contains? (:plugins @doc) plugin)
      (swap! doc update-in [:plugins] conj-set plugin))))

(defmethod pl/unregister-plugin! :global
  [app plugin]
  (when (contains? (:plugins @app) plugin)
    (swap! app update-in [:plugins] disj plugin)))

(defmethod pl/unregister-plugin! :local
  [app plugin]
  (when-let [doc (current-document @app)]
    (when (contains? (:plugins @doc) plugin)
      (swap! doc update-in [:plugins] disj plugin))))

(defn load-lang-plugins!
  "Loads the plugins associated with the language assigned
to the doc."
  [app doc]
  (let [lang-id (-> @doc :lang :id)]
    (doseq [plugin-name ((config @app :lang-plugins) lang-id)]
      (pl/load-plugin! app plugin-name))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Document operations

(defn current-document
  "Returns the atom that contains the current document."
  [app]
  (:current-document app))

(defn switch-document
  "Changes the current document to the one specified.
If doc is not nil then it checks if the document exists 
within the opened documents."
  [{documents :documents :as app} doc]
  (or (and (or (nil? doc) (documents doc))
           (assoc app :current-document doc))
      app))

(defn find-doc-by
  "Returns the first document that satisfies pred."
  [{docs :documents} pred]
  (->> docs
    (filter (comp pred deref))
    first))

(defn find-doc-by-name
  "Returns the document that has the supplied name."
  [app x]
  (find-doc-by app #(= x (:name %))))

(defn same-file?
  "Checks if the two files supplied are the same."
  [^File x ^File y]
  (when (and x y)
    (= (.getCanonicalPath x) (.getCanonicalPath y))))

(defn find-doc-by-path
  "Returns the document that has the supplied path."
  [app x]
  (let [x (io/file x)]
    (find-doc-by app #(same-file? (-> % :path io/file) x))))

(defn new-document
  "Creates a new document, adds it to the document
collection and sets it as the current-document."
  ([app]
   (new-document app (default-lang app)))
  ([app lang]
   (let [doc (atom (doc/document lang))]
     (-> app
       (update-in [:documents] conj doc)
       (switch-document doc)))))

(defn open-document
  "Opens a document from an existing file
and adds it to the openened documents map."
  ([app path]
   (let [langs (-> app :langs vals)
         default (default-lang app)]
     (open-document app path (lang/resolve-lang path langs default))))
  ([app path lang]
   {:pre [path]}
   (if-let [opened-doc (find-doc-by-path app path)]
     (switch-document app opened-doc)
     (let [doc (atom (doc/document lang path))]
       (-> app
         (update-in [:documents] conj doc)
         (switch-document doc)
         (config :current-dir path))))))

(defn close-document
  "Closes a document and removes it from the opened
documents collection."
  [{current :current-document :as app} doc]
  (let [current (when (not= current doc) current)]
    (when doc
      (doc/close doc))
    (-> app
      (update-in [:documents] #(disj % doc))
      (switch-document current))))

(defn save-document
  "Saves a document to a file."
  [app doc]
  (when doc
    (swap! doc doc/save))
  (config app :current-dir (:path @doc)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Configuration

(defn- load-config
  "Loads the configuration file form the specified path
or the default path if no path is given."
  ([app]
    (load-config app "./lab.config"))
  ([app path]  
    (let [exists  (and path (-> (io/file path) .exists))
          config  (when exists (load-string (slurp path)))]
      (update-in app [:config] merge config))))

(defn config
  "Gets or sets the value for key from the 
app's configuration map."
  ([app k]
    (get-in app [:config k]))
  ([app k v]
    (assoc-in app [:config k] v)))
    
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Initialization

(defn init
  "Initializes an instance of an application."
  [config-path]
  (let [app (atom default-app)]
    ; Load configuration from the file specified.
    (swap! app load-config config-path)
    ; Load core and other plugins specified in the config.
    (doseq [plugin-name (mapcat (partial config @app)
                                [:core-plugins :plugins])]
      (pl/load-plugin! app plugin-name))
    app))
