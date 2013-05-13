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

(defn app []
  {:config            default-config
   :documents         {}
   :current-document  nil
   :workspace         (ws/workspace)})
  
(defn current-document
  "Returns the atom that contains the current document."
  [app-ref]
  {:pre [(instance? clojure.lang.Atom app-ref)]}
  (:current-document @app-ref))

(defn open-document
  "Opens a document from an  existing file 
  and adds it to the openened documents map."
  [{documents :documents :as app} path]
  (let [doc   (atom (doc/document path))
        name  (:name @doc)]
    (if (documents name)
      app
      (assoc app :documents (assoc documents name doc)
                 :current-document doc))))

(defn close-document
  "Closes a document and removes it from the opened
  documents collection."
  [{documents :documents current :current-document :as app} name]
  (let [doc     (documents name)
        current (when (not= current doc) current)]
    (when doc
      (doc/close doc))
    (assoc app :documents (dissoc documents name)
               :current-document current)))

(defn save-document
  "Saves a document to a file."
  [{documents :documents :as app} name]
  (let [doc (documents name)]
    (when doc
      (doc/save doc)))
  app)

(defn switch-document
  "Changes the current document to the one with the
  specified id."
  [{documents :documents :as app} name]
  (let [doc (documents name)]
    (or (and doc (assoc app :current-document doc))
        app)))

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
      ui/init
      (load-config config-path)
      load-plugins))
