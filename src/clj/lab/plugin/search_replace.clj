(ns lab.plugin.search-replace
  (:require [clojure.java.io :as io]
            [lab.core.plugin :as plugin]
            [lab.core.keymap :as km]
            [lab.model.protocols :as model]
            [lab.util :as util]
            [lab.ui.core :as ui]
            [lab.ui.templates :as tplts]
            lab.plugin.main-ui)
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
        (lab.plugin.main-ui/open-document app (.getCanonicalPath file)))))))

(defn- file-label [^File file]
  (str (.getName file) " - [" (.getPath file) "]"))

(defn- search-file [dialog files app e]
  (let [editor (:source e)
        s      (model/text editor)]
    (when (< 2 (count s))
      (let [re     (re-pattern s)
            result (filter #(->> (.getName ^File %) (re-find re)) files)

            node   [:tree-node {:leaf true
                                :listen [:click ::open-document
                                         :key ::open-document]}]
            root   (ui/init [:tree-node {:item ::root}])
            root   (->> result
                     (map #(-> (ui/init node)
                             (ui/attr :item (file-label %))
                             (ui/attr :stuff {:dialog dialog :file %})))
                     (reduce ui/add root))]
        (ui/action
          (ui/update! dialog :tree ui/remove-all)
          (ui/update! dialog :tree ui/add root))))))

(defn- file-explorer-current-dirs [app]
  (let [root  (ui/find @(:ui @app) :#file-explorer-root)
        dirs  (when root (->> (ui/children root)
                           (map #(ui/attr % :item))))]
    (if-not dirs
      (file-seq (io/file "."))
      (apply concat (map file-seq dirs)))))

(defn- search-open-file [app e]
  (let [files  (file-explorer-current-dirs app)
        dialog (atom nil)
        ch     (util/timeout-channel 500 (partial #'search-file dialog files))]
    (ui/action 
      (reset! dialog
            (-> (tplts/search-file-dialog "Search & Open File")
              ui/init
              (ui/update :text-field ui/listen :insert ch)
              (ui/update :text-field ui/listen :delete ch)))
      ;; Show the modal dialog without modifying the atom so that
      ;; there's no retry when the compare-and-set! is done on the atom.
      (ui/update @dialog :dialog ui/attr :visible true))))

(def ^:private keymaps
  [(km/keymap 'lab.plugin.search-replace
     :global
     {:category "Search" :fn ::search-text :keystroke "ctrl f" :name "Text..."}
     {:category "Search" :fn ::search-open-file :keystroke "ctrl alt o" :name "File..."})])

(plugin/defplugin lab.plugin.search-replace
  :keymaps keymaps)
