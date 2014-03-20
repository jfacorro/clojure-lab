(ns lab.plugin.file-explorer
  "Add a global action that creates a tree control with the dirs and files of
the specified root dir."
  (:require [lab.core :as lab]
            [lab.core [keymap :as km]
                      [plugin :refer [defplugin]]
                      [main :refer [open-document]]]
            [lab.model.protocols :as model]
            [lab.util :as util]
            [lab.ui [core :as ui]
                    [templates :as tplts]]
            [clojure.java.io :as io]
            [clojure-watch.core :refer [start-watch]])
  (:import java.io.File))

;;;;;;;;;;;;;;;;;;;;;;
;; Search & Open File

(defn- open-document-dialog
  "Check the event for double click or enter key, if so
open the document associated with the selected item."
  [e]
  (when (or (= 2 (:click-count e))
            (and (= :pressed (:event e)) (= :enter (:description e))))
    (let [node   (:source e)
          app    (:app e)
          {:keys [dialog ^File file]}
                 (ui/stuff node)]
      (when file
        (ui/action
          (ui/update! dialog :dialog ui/attr :visible false)
          (open-document app (.getCanonicalPath file)))))))

(defn- file-label [^File file]
  (str (.getName file) " - [" (.getPath file) "]"))

(defn- current-dirs
  "Looks for the File Explorer tree root. If it is
found then the concatenated file-seqs for the loaded
directories are returned. Otherwise the file-seq for the
\".\" directory is returned."
  [app]
  (let [root  (ui/find @(:ui @app) :#file-explorer-root)
        dirs  (when root (->> (ui/children root)
                           (map #(ui/attr % :item))))]
    (if-not dirs
      (file-seq (io/file "."))
      (apply concat (map file-seq dirs)))))

(def ^:private max-files 25)

(defn- search-file
  "Checks the search text in the field and finds the 
files for which any part of its full path matches the
search string. Finally it removes all the previos items
and adds new found ones."
  [e]
  (let [field  (:source e)
        dialog (:dialog (ui/stuff field))
        s      (model/text field)]
    (if (< (count s) 3)
      (ui/action (ui/update! dialog :#results ui/remove-all))
      (let [files  (current-dirs (:app e))
            re     (re-pattern s)
            result (->> files
                     (filter #(re-find re (.getCanonicalPath ^File %)))
                     (take max-files)
                     sort)
            node   [:tree-node {:leaf true
                                :listen [:click ::open-document-dialog
                                         :key ::open-document-dialog]}]
            root   (->> result
                     (map #(-> (ui/init node)
                             (ui/attr :item (file-label %))
                             (ui/attr :stuff {:dialog dialog :file %})))
                     (reduce ui/add (ui/init [:tree-node {:item ::root}])))]
        (ui/action
          (ui/update! dialog :#results ui/remove-all)
          (ui/update! dialog :#results ui/add root))))))

(defn- search-open-file
  "Creates a dialog with a text field that allows to search
for files whose complete path match the text provided. If the
File Explorer is open then the files are searched in the directories
loaded, otherwise the '.' directory is used."
  [e]
  ;; Add ESC as an exit dialog key.
  (let [dialog (atom nil)
        ch     (util/timeout-channel 200 #'search-file)
        owner  (-> e :app deref :ui deref)]
    (ui/action
      (reset! dialog
            (-> (tplts/search-file-dialog owner "Search & Open File")
              ui/init
              (ui/update [:#search-file :text-field]
                         #(-> %
                            (ui/attr :stuff {:dialog dialog})
                            (ui/listen :insert ch)
                            (ui/listen :delete ch)))))
      ;; Show the modal dialog without modifying the atom so that
      ;; there's no retry when the compare-and-set! is done on the atom.
      (ui/update @dialog :#search-file ui/attr :visible true))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Tree nodes creation

(declare tree-node-from-file)

(defn- file-proxy
  "Creates a proxy that overrides the toString method
for the File class so that it returns the (file/directory)'s
name."
  [^File file]
  (proxy [File] [(.getPath file)]
    (toString []
      (.getName ^File this))))

(defn- hidden?
  "Returns true for files or directories that begin with a dot."
  [^File file]
  (-> file .getName (.startsWith ".")))

(defn- file-node-children
  "Returns a vector of children nodes for the
file specified, which should be a directory."
  [^File file]
  (->> file
    .listFiles
    (filter (comp not hidden?))
    (sort-by (fn [^File x] (if (.isDirectory x) (str " " (.getName x)) (.getName x))))
    (map file-proxy)
    (mapv #(tree-node-from-file % true))))

(defn- lazy-add-to-dir
  "Lazily add the file nodes to this node if it
currently has no children."
  [e]
  (let [app       (:app e)
        ui        (:ui @app)
        node      (:source e)
        id        (ui/attr node :id)
        file      (:file (ui/stuff node))]
    (when (empty? (ui/children node))
      (ui/update! ui (ui/id= id)
        (partial reduce ui/add)
        (file-node-children file)))))

(defn- tree-node-from-file
  "Creates a tree node with the supplied file as its item.
If the arg is a directory, all its children are added
recursively."
  [^File file & [lazy]]
  (if (.isDirectory file)
    (if-not lazy
      (into [:tree-node {:item file}] (file-node-children file))
      (let [id       (ui/genid)]
        [:tree-node {:id id
                     :item file
                     :stuff {:file file}
                     :listen [:expansion ::lazy-add-to-dir]}]))
    [:tree-node {:item file :leaf true}]))

(defn- file-node [path x]
  (as-> (ui/attr x :item) item
    (= path (and item (instance? File item) (.getCanonicalPath ^File item)))))

(defn- handle-file-change [app event path]
  (let [file   ^File (file-proxy (io/file path))
        parent (.getParent file)
        ui     (:ui @app)]
    (case event
      :delete
        (when-let [node (ui/find @ui [:#file-explorer [:tree-node (partial file-node path)]])]
          (ui/action 
            (ui/update! ui (ui/parent (ui/attr node :id)) ui/remove node)))
      :create
        (let [node (ui/find @ui [:#file-explorer [:tree-node (partial file-node parent)]])]
          (ui/action 
            (ui/update! ui (ui/id= (ui/attr node :id))
                           ui/add (tree-node-from-file file true)))))))

(defn load-dir
  "Loads the file tree for the given path. If its a file
then the parent directory is considered the root of the 
tree. Returns a tree node."
  [root-dir]
  (let [root ^File (file-proxy root-dir)]
    (tree-node-from-file root false)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Open document handlers

(defn- open-document-tree
  "Opens the document that's selected in the tree."
  [app tree]
  (let [ui      (:ui @app)
        tree    (ui/find @ui (ui/id= (ui/attr tree :id)))
        node    (ui/find @ui (ui/id= (ui/selection tree)))
        ^File file (ui/attr node :item)]
    (when (and node (.isFile file))
      (open-document app (.getCanonicalPath file)))))

(defn- open-document-tree-click
  "Handler for the click event of an item in the tree."
  [{:keys [app source click-count] :as e}]
  (when (= click-count 2)
    (open-document-tree app source)))

(defn- open-document-tree-enter
  [{:keys [app event source description] :as e}]
  (when (and (= :pressed event) (= description :enter))
    (open-document-tree app source)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; File explorer tab creation

(defn- stop-all-watches! [app]
  (let [ui        (:ui @app)
        watchers  (-> (ui/find @ui :#file-explorer) (ui/attr :stuff) :watches)]
    ;; Cancel all directory watches
    (doseq [w watchers] (future-cancel w))
    ;; Empty the watches collection
    (ui/update! ui :#file-explorer ui/update-attr :stuff assoc-in [:watches] nil)))

(defn- close-file-explorer [e]
  (stop-all-watches! (:app e))
  (tplts/close-tab e))

(defn- file-explorer
  [app]
  (-> (tplts/tab "file-explorer")
    (ui/update :tab ui/attr :stuff {:close-tab close-file-explorer})
    (ui/update :tab
               ui/update-attr :header
               ui/update :label ui/attr :text "File Explorer")
    (ui/add [:scroll
              [:tree {:hide-root true
                      :listen [:click ::open-document-tree-click
                               :key ::open-document-tree-enter]}
                [:tree-node {:id "file-explorer-root" :item ::root}]]])
    (ui/apply-stylesheet (:styles @app))))

(defn- watch-dir!
  "Creates a future in which the a watching service is run and
adds the future to the :stuff in the file explorer tab. Futures
should be cancelled when the tab is closed."
  [app ^File dir]
  (let [f (future (start-watch [{:path (.getCanonicalPath dir)
                                 :event-types [:create :delete]
                                 :callback (partial #'handle-file-change app)
                                 :options {:recursive true}}]))]
    (ui/update! (:ui @app)
                :#file-explorer
                ui/update-attr :stuff update-in [:watches] conj f)))

(defn- open-directory
  "Create the file explorer tab if it doesn't exist and add
the directory structure to it, otherwise just add the directory
structure."
  [e]
  (let [app          (:app e)
        ui           (:ui @app)
        dir          (lab/config @app :current-dir)
        dir-dialog   (ui/init (tplts/directory-dialog "Open Directory" dir @ui))
        [result dir] (ui/attr dir-dialog :result)
        dir          ^File (when dir (io/file (.getCanonicalPath ^File dir)))]
    (when (= result :accept)
      (swap! app lab/config :current-dir (.getCanonicalPath dir))
      (when-not (ui/find @ui :#file-explorer)
        (ui/update! ui :#left ui/add (file-explorer app)))
      (ui/update! ui [:#file-explorer :#file-explorer-root] ui/add (load-dir dir))
      (watch-dir! app dir))))

(def ^:private keymaps
  [(km/keymap (ns-name *ns*)
              :global
              {:category "File" :name "Open Dir" :fn ::open-directory :keystroke "ctrl d"}
              {:category "File" :name "Search & Open" :fn ::search-open-file :keystroke "ctrl alt o"})])

(defplugin lab.plugin.file-explorer
  :type :global
  :keymaps keymaps)
