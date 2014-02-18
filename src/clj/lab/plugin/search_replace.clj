(ns lab.plugin.search-replace
  (:require [clojure.java.io :as io]
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
            result (filter #(re-find re (.getName ^File %)) files)

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

(def file-open-keymap
  (km/keymap 'file-open-dialog
    :local
    {:keystroke "down" :fn #(prn (dissoc %2 :source)) :name "Select next node"}
    {:keystroke "up" :fn #(prn (dissoc %2 :source)) :name "Select previous node"}
    {:keystroke "enter" :fn #(prn (dissoc %2 :source)) :name "Select previous node"}))

(defn handle-key [dialog app e]
  (when (= :pressed (:event e))
    (let [kss  (ui/key-stroke (dissoc e :source))
          cmd  (apply km/find-or file-open-keymap kss)]
      (when cmd
        ((:fn cmd) app e)))))

(defn- search-open-file [app e]
  ;; Add ESC as an exit dialog key.
  (let [dialog (atom nil)
        ch     (util/timeout-channel 500 (partial #'search-file dialog))]
    (ui/action
      (reset! dialog
            (-> (tplts/search-file-dialog "Search & Open File")
              ui/init
              (ui/update :text-field ui/listen :key (partial #'handle-key dialog))
              (ui/update :text-field ui/listen :insert ch)
              (ui/update :text-field ui/listen :delete ch)))
      ;; Show the modal dialog without modifying the atom so that
      ;; there's no retry when the compare-and-set! is done on the atom.
      (ui/update @dialog :dialog ui/attr :visible true))))

(defn- search-text [app e]
  (let [ui      (:ui @app)
        editor  (main-ui/current-text-editor @ui)]
    (when editor
      (let [dialog (ui/init (tplts/search-text-dialog "Search Text"))]
        (ui/attr dialog :visible true)
        (prn ::search-text)))))

(def ^:private keymaps
  [(km/keymap 'lab.plugin.search-replace
     :global
     {:category "Search" :fn ::search-text :keystroke "ctrl f" :name "Text..."}
     {:category "Search" :fn ::search-open-file :keystroke "ctrl alt o" :name "File..."})])

(plugin/defplugin lab.plugin.search-replace
  :keymaps keymaps)
