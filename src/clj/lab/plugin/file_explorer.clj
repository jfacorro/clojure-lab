(ns lab.plugin.file-explorer
  "Add a global action that creates a tree control with the dirs and files of
the specified root dir."
  (:use [lab.core.plugin :only [defplugin]])
  (:require [lab.core :as lab]
            [lab.core.keymap :as km]
            [lab.plugin.main-ui :as main]
            [lab.ui.core :as ui]
            [lab.ui.templates :as tplts]
            [clojure.java.io :as io]))

(declare tree-node-from-file)

(defn file-proxy
  "Creates a proxy that overrides the toString method
for the File class so that it returns the (file/directory)'s
name."
  [^java.io.File file]
  (proxy [java.io.File] [(.getPath file)]
    (toString []
      (.getName ^java.io.File this))))

(defn- hidden?
  "Returns true for files or directories that begin with a dot."
  [^java.io.File file]
  (-> file .getName (.startsWith ".")))

(defn- file-node-children
  "Returns a vector of children nodes for the
file specified, which should be a directory."
  [app ^java.io.File file]
  (->> file
    .listFiles
    (filter (comp not hidden?))
    (sort-by #(if (.isDirectory ^java.io.File %) (str " " (.getName ^java.io.File  %)) (.getName ^java.io.File %)))
    (map file-proxy)
    (mapv #(tree-node-from-file app % true))))

(defn- lazy-add-to-dir
  "Lazily add the file nodes to this node if it
currently has no children."
  [e]
  (let [app       (:app e)
        ui        (:ui @app)
        node      (:source e)
        id        (ui/attr node :id)
        file      (:file (ui/attr node :stuff))]
    (when (empty? (ui/children node))
      (ui/update! ui (ui/selector# id)
        (partial reduce ui/add)
        (file-node-children app file)))))

(defn- tree-node-from-file
  "Creates a tree node with the supplied file as its item.
If the arg is a directory, all its children are added
recursively."
  [app ^java.io.File file & [lazy]]
  (if (.isDirectory file)
    (if-not lazy
      (into [:tree-node {:item file}] (file-node-children app file))
      (let [id       (ui/genid)]
        [:tree-node {:id id
                     :item file
                     :stuff {:file file}
                     :listen [:expansion ::lazy-add-to-dir]}]))
    [:tree-node {:item file :leaf true}]))

(defn load-dir
  "Loads the file tree for the given path. If its a file
then the parent directory is considered the root of the 
tree. Returns a tree node."
  [app root-dir]
  (let [root  ^java.io.File (file-proxy root-dir)
        root  (if (.isDirectory root) root (.getParentFile root))]
    (tree-node-from-file app root false)))

(defn- open-document-tree
  "Opens the document that's selected in the tree."
  [app tree]
  (let [ui      (:ui @app)
        tree    (ui/find @ui (ui/selector# (ui/attr tree :id)))
        node    (ui/find @ui (ui/selector# (ui/selection tree)))
        ^java.io.File file (ui/attr node :item)]
    (when (and node (.isFile file))
      (main/open-document app (.getCanonicalPath file)))))

(defn- open-document-tree-click
  "Handler for the click event of an item in the tree."
  [{:keys [app source click-count] :as e}]
  (when (= click-count 2)
    (open-document-tree app source)))

(defn- open-document-tree-enter
  [{:keys [app event source description] :as e}]
  (when (and (= :pressed event) (= description :enter))
    (open-document-tree app source)))

(defn- file-explorer
  [app]
  (-> (tplts/tab "file-explorer")
    (ui/update :label ui/attr :text "File Explorer")
    (ui/add [:scroll {:border :none}
              [:tree {:hide-root true
                      :listen [:click ::open-document-tree-click
                               :key ::open-document-tree-enter]}
                [:tree-node {:id "file-explorer-root" :item ::root}]]])
    (ui/apply-stylesheet (:styles @app))))

(defn- add-dir [tab app dir]
  (let [root   (ui/find tab :#file-explorer-root)]
    ;; Since the root is not updated automatically when
    ;; nodes are added, we need to remove the root and add
    ;; it again.
    (-> tab
      (ui/update :tree ui/remove root)
      (ui/update :tree ui/add (ui/add root (load-dir app dir))))))

(defn- open-directory
  "Create the file explorer tab if it doesn't exist and add
the directory structure to it, otherwise just add the directory
structure."
  [e]
  (let [app          (:app e)
        ui           (:ui @app)
        dir          (lab/config @app :current-dir)
        dir-dialog   (ui/init (tplts/directory-dialog "Open Directory" dir))
        [result dir] (ui/attr dir-dialog :result)
        dir          ^java.io.File (when dir (io/file (.getCanonicalPath ^java.io.File dir)))]
    (when (= result :accept)
      (swap! app lab/config :current-dir (.getCanonicalPath dir))
      (if (ui/find @ui :#file-explorer)
        (ui/update! ui :#file-explorer add-dir app dir)
        (ui/update! ui :#left ui/add (-> (file-explorer app) (add-dir app dir)))))))

(def ^:private keymaps
  [(km/keymap (ns-name *ns*)
              :global
              {:category "Project" :name "Open..." :fn #'open-directory :keystroke "ctrl P"})])

(defplugin lab.plugin.file-explorer
  :keymaps keymaps)
