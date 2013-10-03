(ns lab.app
  (:refer-clojure :exclude [name])
  (:require [lab.model [workspace :as ws]
                       [project :as pj]
                       [document :as doc]]
            [lab.ui :as ui]
            [clojure.java.io :as io]))

(def default-config
  '{:name "Clojure Lab"
   :core-plugins [lab.ui
                  lab.core
                  lab.clojure.project
                  lab.clojure.document]
   :plugins-dir "plugins"})

(defn app
  "Returns a new app with nothing initialized and a
  default configuration."
  []
  {:config            default-config
   :documents         #{}
   :current-document  nil
   :workspace         (ws/workspace)})
  
(defn current-document
  "Returns the atom that contains the current document."
  [app]
  (:current-document app))

(defn switch-document
  "Changes the current document to the one with the
  specified id."
  [{documents :documents :as app} doc]
    (or (and doc (assoc app :current-document doc))
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
  (find-doc-by app #(-> % doc/name (= x))))

(defn same-file?
  "Checks if the two files supplied are the same."
  [x y]
  (when (and x y)
    (= (.getCanonicalPath x) (.getCanonicalPath y))))

(defn find-doc-by-path
  "Returns the document that has the supplied path."
  [app x]
  (let [x (io/file x)]
    (find-doc-by app #(-> % doc/file (same-file? x)))))

(defn new-document 
  "Creates a new document, adds it to the document
  collection and sets it as the current-document."
  [{documents :documents :as app}]
  (let [doc (atom (doc/document))]
    (assoc app :documents (conj documents doc)
               :current-document doc)))

(defn open-document
  "Opens a document from an existing file 
  and adds it to the openened documents map."
  [{documents :documents :as app} path]
  {:pre [path]}
  (let [doc (atom (doc/document :path path))]
    (if (find-doc-by-path app path)
      (switch-document app doc)
      (assoc app :documents (conj documents doc)
                 :current-document doc))))

(defn close-document
  "Closes a document and removes it from the opened
  documents collection."
  [{documents :documents current :current-document :as app} doc]
  (let [current (when (not= current doc) current)]
    (when doc
      (doc/close doc))
    (assoc app :documents (disj #{doc} documents)
               :current-document current)))

(defn save-document
  "Saves a document to a file."
  [{documents :documents :as app} doc]
  (when doc
    (doc/save doc))
  app)

(defn open-project
  "Opens a project from an existing file
  and adds it to the current workspace."
  [{ws :workspace :as app} path]
  (let [p (pj/project path)]
    (assoc app :workspace (ws/add-project ws p))))

(defn save-project
  "Saves the project to its associated file."
  [{ws :workspace :as app} name]
  (let [p (ws/get-project ws name)]
    (pj/save p))
  app)

(defn load-plugins
  "Loads all files from the extension path specified in 
  the config map."
  [{config :config :as app}]
  app)

(defn load-config
  "Loads the configuration file form the specified path
  or the default path if no path is given."
  ([app]
    (load-config app "./lab.config"))
  ([app path]
    (let [exists  (and path (-> (io/file path) .exists))
          config  (when exists (load-string (slurp path)))]
      (update-in app [:config] merge config))))

(defn init
  "Initializes an instance of an application."
  [config-path]
  (-> (app)
      ;ui/init
      (load-config config-path)
      load-plugins))
