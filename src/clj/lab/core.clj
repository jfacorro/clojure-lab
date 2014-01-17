(ns lab.core
  (:refer-clojure :exclude [name])
  (:require [lab.model [workspace :as ws]
                       [project :as pj]
                       [document :as doc]]
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
   :core-plugins  '[lab.plugin.main-ui
                    lab.plugin.notifier]
   :plugins       '[lab.plugin.file-tree
                    lab.plugin.markdown-lang
                    lab.plugin.clojure-lang
                    lab.plugin.clojure-outline]
   :plugins-dir   "plugins"
   :current-dir   "."})

(def default-app
  "Returns a new app with nothing initialized and a
default configuration."
  {:name              "Clojure Lab"
   :config            default-config
   :documents         #{}
   :current-document  nil
   :langs             {:plain-text lang/plain-text}
   :default-lang      :plain-text
   :keymap            nil})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Language

(defn lang
  "Returns the language registered with the specified key in the langs map."
  [app k]
  (->> app :langs k))

(defn default-lang
  "Returns the default language."
  [app]
  (->> app :default-lang (lang app)))

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
  (let [doc (current-document app)]
    (swap! doc update-in [:keymap] km/append keymap)))

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
  (find-doc-by app #(-> % doc/name (= x))))

(defn same-file?
  "Checks if the two files supplied are the same."
  [^File x ^File y]
  (when (and x y)
    (= (.getCanonicalPath x) (.getCanonicalPath y))))

(defn find-doc-by-path
  "Returns the document that has the supplied path."
  [app x]
  (let [x (io/file x)]
    (find-doc-by app #(same-file? (doc/file %) x))))

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
  (let [doc        (atom (doc/document lang path))
        opened-doc (find-doc-by-path app path)]
    (if opened-doc
      (switch-document app opened-doc)
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
  (config app :current-dir (doc/path @doc)))

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
    (doseq [plugin-type [:core-plugins :plugins]]
      (pl/load-plugins! app (get-in @app [:config plugin-type])))
    
    app))
