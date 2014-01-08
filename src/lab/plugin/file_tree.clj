(ns lab.plugin.file-tree
  "Add an global action that creates a tree control with the dirs and files of
the specified root dir."
  (:use [lab.core.plugin :only [defplugin]])
  (:require [lab.core :as lab]
            [lab.core.keymap :as km]
            [lab.plugin.main-ui :as main]
            [lab.ui.core :as ui]
            [lab.ui.templates :as tpl]
            [clojure.java.io :as io]))

(defn file-proxy
  "Creates a proxy that overrides the toString method
for the File class so that it returns the (file/directory)'s
name."
  [file]
  (proxy [java.io.File] [(.getPath file)]
    (toString []
      (.getName this))))

(defn- hidden?
  "Returns true for files or directories that begin with a dot."
  [file]
  (-> file .getName (.startsWith ".")))

(defn- tree-node-from-file
  "Creates a tree node with the supplied file as its item.
If the arg is a directory, all its children are added
recursively."
  [file]
  (if (.isDirectory file)
    (let [children (->> file .listFiles 
                     (filter (comp not hidden?))
                     (sort-by #(if (.isDirectory %) (str " " (.getName %)) (.getName %)))
                     (map file-proxy))]
      (into [:tree-node {:item file}]
              (->> children (map tree-node-from-file) vec)))
    [:tree-node {:item file}]))

(defn load-dir
  "Loads the directory tree for the given path. If its a file
then the parent directory is considered the root of the 
tree. Returns a tree node."
  [root-path]
  (let [root  (io/file root-path)
        root  (if (.isDirectory root) root (.getParentFile root))]
    (tree-node-from-file root)))

(defn- open-document-tree [app tree]
  (let [ui        (:ui @app)
        node      (ui/selected tree)
        ^java.io.File file (ui/attr node :item)]
    (when (and node (.isFile file))
      (main/open-document app (.getCanonicalPath file)))))

(defn- open-document-tree-click
  "Handler for the click event of an item in the tree."
  [app {:keys [source click-count]}]
  (when (= click-count 2)
    (open-document-tree app source)))

(defn- open-document-tree-enter [app {:keys [event source description] :as e}]
  (when (and (= :pressed event) (= description :enter))
    (open-document-tree app source)))

(defn- file-tree [app dir]
  (-> app
    (tpl/tab (.getName dir))
    (ui/add [:tree {:id      "file-tree"
                    :on-click (partial #'open-document-tree-click app)
                    :on-key (partial #'open-document-tree-enter app)}
                    (load-dir dir)])))

(defn- open-project
  [app _]
  (let [file-dialog   (ui/init [:file-dialog {:type           :open, 
                                              :selection-type :dir-only, 
                                              :visible        true, 
                                              :title          "Open Directory"
                                              :current-dir    (lab/config @app :current-dir)}])
        [result dir] (ui/attr file-dialog :result)]
    (when dir
      (ui/update! (:ui @app) :#left-controls ui/add (file-tree app dir)))))

(def ^:private keymaps
  [(km/keymap (ns-name *ns*)
              :global
              {:category "Project" :name "Open..." :fn #'open-project :keystroke "ctrl P"})])

(defplugin file-tree
  :keymaps keymaps)