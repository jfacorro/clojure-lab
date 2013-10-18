(ns lab.app
  (:refer-clojure :exclude [name])
  (:require [lab.model [workspace :as ws]
                       [project :as pj]
                       [document :as doc]]
            [lab.core.keymap :as km]
            [lab.core.plugin :as pl]
            [clojure.java.io :as io]))

(declare current-document open-document save-document close-document)

(def default-config
  {:name          "Clojure Lab"
   :core-plugins  '[lab.ui]
   :plugins       []
   :plugins-dir   "plugins"})

(def default-app
  "Returns a new app with nothing initialized and a
default configuration."
  {:config            default-config
   :documents         #{}
   :current-document  nil
   :langs             {}
   :keymap            {"ctrl O"  #'open-document
                       "ctrl S"  #'save-document
                       "ctrl W"  #'close-document}})

(defmethod km/register-multi :global
  [app keymap]
  (update-in app [:keymap] km/append keymap))

(defmethod km/register-multi :lang
  [app {lang :lang :as keymap}]
  (update-in app [:langs lang :keymap] km/append keymap))

(defmethod km/register-multi :local
  [app keymap]
  (let [doc (current-document app)]
    (swap! doc update-in [:keymap] km/append keymap)))

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
  [app]
  (let [doc (atom (doc/document))]
    (-> app
      (update-in [:documents] conj doc)
      (assoc :current-document doc))))

(defn open-document
  "Opens a document from an existing file 
and adds it to the openened documents map."
  [app path]
  {:pre [path]}
  (let [doc (atom (doc/document :path path))]
    (if (find-doc-by-path app path)
      (switch-document app doc)
      (-> app
        (update-in [:documents] conj doc)
        (assoc :current-document doc)))))

(defn close-document
  "Closes a document and removes it from the opened
documents collection."
  [{current :current-document :as app} doc]
  (let [current (when (not= current doc) current)]
    (when doc
      (doc/close doc))
    (-> app
      (update-in [:documents] disj #{doc})
      (assoc :current-document current))))

(defn save-document
  "Saves a document to a file."
  [{documents :documents :as app} doc]
  (when doc
    (doc/save doc))
  app)

(defn- load-config
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
  (let [app (atom default-app)]
    ; Load configuration from the file specified.
    (swap! app load-config config-path)
    
    ; Load core and other plugins specified in the config.
    (doseq [plugin-type [:core-plugins :plugins]]
      (pl/load-plugins! app (get-in @app [:config plugin-type])))
    
    app))

(do (init nil) nil)