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

(defn view-search-file
  "Defines the dialog that's used to search and
open files."
  [owner dialog chan]
  [:dialog {:id "search-file"
            :title "Search & Open File"
            :size  [500 150]
            :modal true
            :owner owner}
   [:panel {:layout [:box :page]}
    [:text-field {:border :none
                  :stuff  {:dialog dialog}
                  :listen [:insert chan
                           :delete chan]}]
    [:panel {:layout :border}
     [:scroll {:border :none}
      [:tree {:id "results" :hide-root true}
       [:tree-node {:item :root}]]]]]])

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

(defn- file-label
  "Returns the file label, trying to get the relative path
  to the current dirs."
  [^File file dirs]
  (let [dirs (map #(.getParent ^File %) dirs)
        path (reduce (fn [x dir]
                       (as-> (util/relativize dir x) rel
                         (if (not= x rel)
                           (reduced rel)
                           x)))
               file
               dirs)]
    (str (.getName file) " - [" (.getPath (io/file path)) "]")))

(defn- current-dirs
  "Looks for the File Explorer tree root and returns
  a sequence of its children's associated directory."
  [app]
  (let [root (ui/find @(:ui @app) :#file-explorer-root)]
    (when root (map #(ui/attr % :item) (ui/children root)))))

(defn- current-files
  "If it is
  found then the concatenated file-seqs for the loaded
  directories are returned. Otherwise the file-seq for the
  \".\" directory is returned."
  [app]
  (if-let [dirs (current-dirs app)]
    (apply concat (map file-seq dirs))
    (file-seq (io/file "."))))

(def ^:private max-files 25)

(defn- search-file
  "Checks the search text in the field and finds the 
  files for which any part of its full path matches the
  search string. Finally it removes all the previous items
  and adds new found ones."
  [{:keys [source app] :as e}]
  (let [dialog (:dialog (ui/stuff source))
        s      (model/text source)]
    (if (< (count s) 3)
      (ui/action (ui/update! dialog :#results ui/remove-all))
      (let [files  (current-files app)
            dirs   (current-dirs app)
            re     (re-pattern s)
            result (->> files
                     (filter #(.isFile ^File %))
                     (filter #(re-find re (.getCanonicalPath ^File %)))
                     (take max-files)
                     sort)
            node   [:tree-node {:leaf true
                                :listen [:click ::open-document-dialog
                                         :key ::open-document-dialog]}]
            root   (->> result
                     (map #(-> (ui/init node)
                            (ui/attr :item (file-label % dirs))
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
  [{:keys [app] :as e}]
  ;; Add ESC as an exit dialog key.
  (let [dialog (atom nil)
        ch     (util/timeout-channel 200 #'search-file)
        ui     (:ui @app)]
    (ui/action
      (-> dialog
        (reset! (ui/init (view-search-file @ui dialog ch)))
        (ui/apply-stylesheet (:styles @app))
        ;; Show the modal dialog without modifying the atom so that
        ;; there's no retry when the compare-and-set! is done on the atom.
        (ui/attr :visible true)))))

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
  [{:keys [app source] :as e}]
  (let [ui        (:ui @app)
        id        (ui/attr source :id)
        {:keys [file loaded]} (ui/stuff source)]
    (when-not loaded
      (ui/update! ui (ui/id= id)
                  #(-> %
                       (ui/add-all (file-node-children file))
                       (ui/update-attr :stuff assoc :loaded true))))))

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
  [{:keys [app source] :as e}]
  (let [ui      (:ui @app)
        tree    (ui/find @ui (ui/id= (ui/attr source :id)))
        node    (ui/find @ui (ui/id= (ui/selection tree)))
        ^File file (ui/attr node :item)]
    (when (and node (.isFile file))
      (open-document app (.getCanonicalPath file)))))

(defn- open-document-tree-click
  "Handler for the click event of an item in the tree."
  [{:keys [app source click-count] :as e}]
  (when (= click-count 2)
    (open-document-tree e)))

(declare stop-watch!)

(defn- remove-from-tree
  [{:keys [app source] :as e}]
  (let [ui      (:ui @app)
        id      (ui/attr source :id)
        root    (ui/find @ui :#file-explorer-root)
        node    (ui/find @ui (ui/id= (ui/selection source)))
        ^File file (ui/attr node :item)]
    (when (and node
            (.isDirectory file)
            ((set (ui/children root)) node))
      (stop-watch! app (ui/attr node :item))
      (ui/update! ui [(ui/id= id) :#file-explorer-root] ui/remove node))))

(def ^:private file-explorer-keymap
  (km/keymap "File Explorer"
    :local
    {:keystroke "enter" :fn ::open-document-tree :name "Open File"}
    {:keystroke "delete" :fn ::remove-from-tree :name "Remove from Explorer"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; File explorer tab creation

(defn- stop-all-watches!
  [app]
  (let [watches  (::watches @app)]
    ;; Cancel all directory watches
    (doseq [w watches] (future-cancel w))
    ;; Empty the watches collection
    (swap! app dissoc ::watches)))

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
    (ui/add 
      [:panel {:layout [:box :page]
               :background 0x333333}
       [:toolbar {:background 0x333333
                  :floatable false
                  :border :none}
        [:button {:text "Add dir"
                  :icon "add.png"
                  :color 0xFFFFFF
                  :border :none
                  :padding 0
                  :transparent true
                  :listen [:click ::open-directory]}]]
       [:scroll
        [:tree {:hide-root true
                :listen [:click ::open-document-tree-click
                         :key file-explorer-keymap]}
         [:tree-node {:id "file-explorer-root" :item ::root}]]]])
    (ui/apply-stylesheet (:styles @app))))

(defn- stop-watch!
  [app dir]
  (let [watches (::watches @app)
        watch   (first (filter #(util/same-file? (:dir (meta %)) dir) watches))]
    (when watch
      (future-cancel watch))
    (swap! app assoc ::watches (remove #{watch} watches))))

(defn- watch-dir!
  "Creates a future in which the a watching service is run and
adds the future to the :stuff in the file explorer tab. Futures
should be cancelled when the tab is closed."
  [app ^File dir]
  (let [f (future (start-watch [{:path (.getCanonicalPath dir)
                                 :event-types [:create :delete]
                                 :callback (partial #'handle-file-change app)
                                 :options {:recursive true}}]))]
    (swap! app update-in [::watches] conj (with-meta f {:dir dir}))))

(defn- create-file-explorer! [app]
  (let [ui (:ui @app)]
    (when-not (ui/find @ui :#file-explorer)
      (ui/update! ui :#left ui/add (file-explorer app)))))

(defn- open-directory
  "Create the file explorer tab if it doesn't exist and add
the directory structure to it, otherwise just add the directory
structure."
  [{:keys [app] :as e}]
  (let [ui           (:ui @app)
        dir          (lab/config @app :current-dir)
        dir-dialog   (ui/init (tplts/directory-dialog "Open Directory" dir @ui))
        [result dir] (ui/attr dir-dialog :result)
        dir          ^File (when dir (io/file (.getCanonicalPath ^File dir)))]
    (when (= result :accept)
      (swap! app lab/config :current-dir (.getCanonicalPath dir))
      (create-file-explorer! app)
      (ui/update! ui [:#file-explorer :#file-explorer-root] ui/add (load-dir dir))
      (watch-dir! app dir))))

(def ^:private keymaps
  [(km/keymap (ns-name *ns*)
              :global
              {:category "File" :name "Open Dir" :fn ::open-directory :keystroke "ctrl d"}
              {:category "File" :name "Search & Open" :fn ::search-open-file :keystroke "ctrl alt o"})])

(defn- init! [app]
  (create-file-explorer! app))

(defplugin lab.plugin.file-explorer
  :type    :global
  :init!   #'init!
  :keymaps keymaps)
