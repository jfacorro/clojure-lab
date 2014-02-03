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
  [file]
  (proxy [java.io.File] [(.getPath file)]
    (toString []
      (.getName this))))

(defn- hidden?
  "Returns true for files or directories that begin with a dot."
  [file]
  (-> file .getName (.startsWith ".")))

(defn- file-node-children
  "Returns a vector of children nodes for the
file specified, which should be a directory."
  [app file]
  (->> file .listFiles
    (filter (comp not hidden?))
    (sort-by #(if (.isDirectory %) (str " " (.getName %)) (.getName %)))
    (map file-proxy)
    (mapv #(tree-node-from-file app % true))))

(defn- lazy-add-to-dir
  "Lazily add the file nodes to this node if it
currently has no children."
  [app e]
  (let [ui        (:ui @app)
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
  [app file & [lazy]]
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
  (let [root  (file-proxy root-dir)
        root  (if (.isDirectory root) root (.getParentFile root))]
    (tree-node-from-file app root false)))

(defn- open-document-tree [app tree]
  (let [ui      (:ui @app)
        tree    (ui/find @ui (ui/selector# (ui/attr tree :id)))
        node    (ui/find @ui (ui/selector# (ui/selection tree)))
        ^java.io.File file (ui/attr node :item)]
    (when (and node (.isFile file))
      (main/open-document app (.getCanonicalPath file)))))

(defn- open-document-tree-click
  "Handler for the click event of an item in the tree."
  [app {:keys [source click-count]}]
  (when (= click-count 2)
    (open-document-tree app source)))

(defn- open-document-tree-enter
  [app {:keys [event source description] :as e}]
  (when (and (= :pressed event) (= description :enter))
    (open-document-tree app source)))

(defn- file-tree
  [app dir]
  (-> (tplts/tab)
    (ui/update :label ui/attr :text (.getName dir))
    (ui/add [:scroll {:border :none}
              [:tree {:listen [:click ::open-document-tree-click 
                               :key ::open-document-tree-enter]}
                (load-dir app dir)]])
    (ui/apply-stylesheet (:styles @app))))

(defn- open-project
  [app _]
  (let [dir          (lab/config @app :current-dir)
        dir-dialog  (ui/init (tplts/directory-dialog "Open Directory" dir))
        [result dir] (ui/attr dir-dialog :result)
        dir          (when dir (io/file (.getCanonicalPath dir)))]
    (when (= result :accept)
      (swap! app lab/config :current-dir (.getCanonicalPath dir))
      (ui/update! (:ui @app) :#left ui/add (file-tree app dir)))))

(def ^:private keymaps
  [(km/keymap (ns-name *ns*)
              :global
              {:category "Project" :name "Open..." :fn #'open-project :keystroke "ctrl P"})])

(defplugin file-explorer
  :keymaps keymaps)
