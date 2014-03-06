(ns lab.plugin.search-replace
  (:require [clojure.java.io :as io]
            [clojure.core.async :as async]
            [lab.core.plugin :as plugin]
            [lab.core.keymap :as km]
            [lab.model.protocols :as model]
            [lab.util :as util]
            [lab.ui.core :as ui]
            [lab.ui.templates :as tplts]
            [lab.plugin.main-ui :as main-ui])
  (:import  [java.nio.file FileSystems]
            [java.io File ]))

(defn- open-document [app e]
  (when (or (= 2 (:click-count e)) 
            (and (= :pressed (:event e)) (= :enter (:description e))))
    (let [node   (:source e)
          stuff  (ui/attr node :stuff)
          dialog (:dialog stuff)
          file   ^File (:file stuff)]
    (when file
      (ui/action
        (ui/update! dialog :dialog ui/attr :visible false)
        (main-ui/open-document app (.getCanonicalPath file)))))))

(defn- file-label [^File file]
  (str (.getName file) " - [" (.getPath file) "]"))

(defn- file-explorer-current-dirs
  "Looks for the File Explorer tree root. If it is
found then the concatenated file-seqs for the loaded
directories are returnes. Otherwise the file-seq for the
\".\" directory is returned."
  [app]
  (let [root  (ui/find @(:ui @app) :#file-explorer-root)
        dirs  (when root (->> (ui/children root)
                           (map #(ui/attr % :item))))]
    (if-not dirs
      (file-seq (io/file "."))
      (apply concat (map file-seq dirs)))))

(defn- search-file
  "Checks the search text in the field and finds the 
files for which any part of its full path matches the
search string. Finally it removes all the previos items
and adds new found ones."
  [dialog app e]
  (let [field (:source e)
        s      (model/text field)]
    (when (< 2 (count s))
      (let [files  (file-explorer-current-dirs app)
            re     (re-pattern s)
            result (filter #(re-find re (.getCanonicalPath ^File %)) files)

            node   [:tree-node {:leaf true
                                :listen [:click ::open-document
                                         :key ::open-document]}]
            root   (->> result
                     (map #(-> (ui/init node)
                             (ui/attr :item (file-label %))
                             (ui/attr :stuff {:dialog dialog :file %})))
                     (reduce ui/add (ui/init [:tree-node {:item ::root}])))]
        (ui/action
          (ui/update! dialog :tree ui/remove-all)
          (ui/update! dialog :tree ui/add root))))))

(defn selected-index
  "Returns the index for the currently selected item 
in the children's tree root node."
  [tree]
  (let [sel-id (ui/selection tree)
        root   (first (ui/children tree))
        node   (ui/find root (ui/selector# sel-id))]
    (when sel-id
      (util/index-of (ui/children root) node))))

(defn select-next-node [dialog app e]
  (let [tree   (ui/find @dialog :tree)
        root   (first (ui/children tree))
        index  (inc (or (selected-index tree) -1))]
    (when (<= index (dec (count (ui/children root))))
      (ui/update! dialog :tree ui/selection index))))

(defn select-prev-node [dialog app e]
  (let [tree   (ui/find @dialog :tree)
        index  (dec (or (selected-index tree) 1))]
    (when (>= index 0)
      (ui/update! dialog :tree ui/selection index))))

(defn open-selected-node [dialog app e]
  (let [tree   (ui/find @dialog :tree)
        sel-id (ui/selection tree)
        node   (ui/find tree (ui/selector# sel-id))]
    (when node
      (open-document app (assoc e :source node)))))

(def file-open-keymap
  (km/keymap 'file-open-dialog
    :local
    {:keystroke "down" :fn #'select-next-node :name "Select next node"}
    {:keystroke "up" :fn #'select-prev-node :name "Select previous node"}
    {:keystroke "enter" :fn #'open-selected-node :name "Open selected node"}))

(defn handle-key [dialog app e]
  (when (= :pressed (:event e))
    (let [kss  (ui/key-stroke (dissoc e :source))
          cmd  (apply km/find-or file-open-keymap kss)]
      (when cmd
        ((:fn cmd) dialog app e)))))

(defn- search-open-file
  "Creates a dialog with a text field that allows to search
for files whose complete path match the text provided. If the
File Explorer is open then the files are searched in the directories
loaded, otherwise the '.' directory is used."
  [app e]
  ;; Add ESC as an exit dialog key.
  (let [dialog (atom nil)
        ch     (util/timeout-channel 500 (partial #'search-file dialog))]
    (ui/action
      (reset! dialog
            (-> (tplts/search-file-dialog (-> @app :ui deref) "Search & Open File")
              ui/init
              (ui/update :text-field ui/listen :key (partial #'handle-key dialog))
              (ui/update :text-field ui/listen :insert ch)
              (ui/update :text-field ui/listen :delete ch)))
      ;; Show the modal dialog without modifying the atom so that
      ;; there's no retry when the compare-and-set! is done on the atom.
      (ui/update @dialog :dialog ui/attr :visible true))))

;;;;;;;;;;;;;;;;;;;;;;
;; Search Text

(defn- search-channel []
  (let [ch (async/chan)]
    (async/go-loop []
      (let [[app e] (async/<! ch)
            {:keys [editor dialog]} (ui/attr (:source e) :stuff)
            ptrn    (-> (ui/find @dialog :text-field) model/text)
            txt     (model/text editor)
            results (util/find-limits ptrn txt)]
        (ui/action
          (doseq [[start end] results]
            (ui/add-highlight editor start end 0x888888)))
        (recur)))
    ch))

(defn- search-text-in-editor
  "Looks for matches of the entered text in the current editor."
  [app e]
  (let [ui      (:ui @app)
        editor  (main-ui/current-text-editor @ui)]
    (when editor
      (let [dialog (atom nil)
            ch     (search-channel)]
        (reset! dialog (-> (tplts/search-text-dialog @ui "Search Text")
                         ui/init
                         (ui/update :button ui/attr :stuff {:dialog dialog :editor editor})
                         (ui/update :button ui/listen :click ch)))
        (ui/attr @dialog :visible true)))))

(def ^:private keymaps
  [(km/keymap 'lab.plugin.search-replace
     :global
     {:category "Search" :fn ::search-text-in-editor :keystroke "ctrl f" :name "Text"}
     {:category "Search" :fn ::search-open-file :keystroke "ctrl alt o" :name "File"})])

(plugin/defplugin lab.plugin.search-replace
  :keymaps keymaps)
