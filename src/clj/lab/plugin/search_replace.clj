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

;;;;;;;;;;;;;;;;;;;;;;
;; Search File

(defn- open-document
  "Check the event for double click or enter key, if so
open the document associated with the selected item."
  [e]
  (when (or (= 2 (:click-count e))
            (and (= :pressed (:event e)) (= :enter (:description e))))
    (let [node   (:source e)
          app    (:app e)
          stuff  (ui/attr node :stuff)
          dialog (:dialog stuff)
          file   ^File (:file stuff)]
      (when file
        (ui/action
          (ui/update! dialog :dialog ui/attr :visible false)
          (main-ui/open-document app (.getCanonicalPath file)))))))

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
        dialog (:dialog (ui/attr field :stuff))
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
                                :listen [:click ::open-document
                                         :key ::open-document]}]
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
        ch     (util/timeout-channel 500 #'search-file)
        owner  (-> e :app deref :ui deref)]
    (ui/action
      (reset! dialog
            (-> (tplts/search-file-dialog owner "Search & Open File")
              ui/init
              (ui/update [:#search-file :text-field] ui/attr :stuff {:dialog dialog})
              (ui/update [:#search-file :text-field] ui/listen :insert ch)
              (ui/update [:#search-file :text-field] ui/listen :delete ch)))
      ;; Show the modal dialog without modifying the atom so that
      ;; there's no retry when the compare-and-set! is done on the atom.
      (ui/update @dialog :dialog ui/attr :visible true))))

;;;;;;;;;;;;;;;;;;;;;;
;; Search Text

(defn- do-search [hls e]
  (let [dialog  (:dialog (ui/attr (:source e) :stuff))
        editor  (:editor (ui/attr @dialog :stuff))
        ptrn    (-> (ui/find @dialog :text-field) model/text)]
    (when-not (empty? ptrn)
      (ui/action
        (doseq [hl @hls] (ui/remove-highlight editor hl))
        (reset! hls (mapv (fn [[start end]] (ui/add-highlight editor start end 0x888888))
                          (util/find-limits ptrn
                                            (model/text editor))))))))
(defn- search-channel
  "Takes an atom that contains the current highlights for the results
and creates a channel in which the search is performed."
  [hls]
  (let [ch  (async/chan)]
    (async/go-loop []
      (when-let [e (async/<! ch)]
          (do-search hls e)
          (recur)))
    ch))

(defn- close-search-text
  "Closes the channel and removes all highlights from the editor."
  [e]
  (let [{:keys [editor chan highlights]} (-> (:source e) (ui/attr :stuff))]
    (ui/action (doseq [hl @highlights] (ui/remove-highlight editor hl)))
    (async/close! chan)))

(defn- search-text-in-editor
  "Looks for matches of the entered text in the current editor."
  [e]
  (let [app     (:app e)
        ui      (:ui @app)
        editor  (main-ui/current-text-editor @ui)]
    (when editor
      (let [dialog (atom nil)
            hls    (atom nil)
            ch     (search-channel hls)]
        (reset! dialog (-> (tplts/search-text-dialog @ui "Search Text")
                         ui/init
                         (ui/update :#search-text ui/attr :stuff {:chan ch :highlights hls :editor editor})
                         (ui/update :#search-text ui/listen :closing ::close-search-text)
                         (ui/update [:#search-text :button] ui/attr :stuff {:dialog dialog})
                         (ui/update [:#search-text :button] ui/listen :click ch)))
        (ui/attr @dialog :visible true)))))

(def ^:private keymaps
  [(km/keymap 'lab.plugin.search-replace
     :global
     {:category "Search" :fn ::search-text-in-editor :keystroke "ctrl f" :name "Text"}
     {:category "Search" :fn ::search-open-file :keystroke "ctrl alt o" :name "File"})])

(plugin/defplugin lab.plugin.search-replace
  :keymaps keymaps)
