(ns lab.plugin.find-replace
  (:require [clojure.java.io :as io]
            [clojure.core.async :as async]
            [lab.core [plugin :as plugin]
                      [keymap :as km]
                      [main :refer [current-text-editor]]]
            [lab.model.protocols :as model]
            [lab.util :as util]
            [lab.ui [core :as ui]
                    [templates :as tplts]])
  (:import  [java.io File]))

;;;;;;;;;;;;;;;;;;;;;;
;; Search Text

(defn- goto-result [{:keys [click-count button] :as e}]
  (when (and (= 2 click-count) (= :button-1 button))
    (let [node   (:source e)
          {:keys [editor position]} (ui/attr node :stuff)]
      (ui/caret-position editor position))))

(defn- line-at [editor pos]
  (let [s     (model/text editor)
        start (util/find-char s pos #{\newline} -1)
        end   (util/find-char s pos #{\newline} 1)]
    (subs s (if start start 0) (if end end (count s)))))

(defn- search-results-node [editor [start end]]
  [:tree-node {:item   (line-at editor start)
               :leaf   true
               :stuff  {:position start :editor editor}
               :listen [:click ::goto-result]}])

(defn- add-search-results! [dialog results]
  (let [editor (:editor (ui/attr @dialog :stuff))
        txt    (model/text editor)
        items  (map (partial search-results-node editor) results)
        root   (-> [:tree-node {:item :root}] ui/init (ui/add-all items))]
    (ui/update! dialog [:dialog :tree] #(-> % ui/remove-all (ui/add root)))))

(defn- find-next
  "Takes the highlights atom "
  [e]
  (let [dialog  (:dialog (ui/attr (:source e) :stuff))
        {:keys [editor highlights]}
                (ui/attr @dialog :stuff)
        ptrn    (model/text (ui/find @dialog :text-field))
        results (when (seq ptrn) (util/find-limits ptrn (model/text editor)))]
    (when results
      (ui/action
        (doseq [hl @highlights] (ui/remove-highlight editor hl))
        (reset! highlights (mapv (fn [[start end]] (ui/add-highlight editor start end 0x888888))
                          results))
        (add-search-results! dialog results)))))

(defn- search-channel
  "Takes an atom that contains the current highlights for the results
and creates a channel in which the search is performed."
  []
  (let [ch  (async/chan)]
    (async/go-loop []
      (when-let [e (async/<! ch)]
          (find-next e)
          (recur)))
    ch))

(defn- close-find-replace
  "Closes the channel and removes all highlights from the editor."
  [e]
  (let [{:keys [editor chan highlights]} (-> (:source e) (ui/attr :stuff))]
    (ui/action (doseq [hl @highlights] (ui/remove-highlight editor hl)))
    (async/close! chan)))

(defn- show-find-replace
  "Looks for matches of the entered text in the current editor."
  [e]
  (let [app     (:app e)
        ui      (:ui @app)
        editor  (current-text-editor @ui)]
    (when editor
      (let [dialog (atom nil)
            ch     (search-channel)]
        (reset! dialog (-> (tplts/find-replace-dialog @ui)
                         ui/init
                         (ui/update :#find-replace-dialog
                                    #(-> % (ui/attr :stuff {:chan ch
                                                            :highlights (atom nil)
                                                            :editor editor})
                                           (ui/listen :closing ::close-find-replace)
                                           (ui/listen :closed ::close-find-replace)))
                         (ui/update :#find-btn
                                    #(-> % (ui/attr :stuff {:dialog dialog})
                                           (ui/listen :click ch)))))
        (ui/attr @dialog :visible true)))))

(def ^:private keymaps
  [(km/keymap 'lab.plugin.find-replace
     :global
     {:category "Edit" :fn ::show-find-replace :keystroke "ctrl f" :name "Find & Replace"})])

(plugin/defplugin lab.plugin.find-replace
  :type    :global
  :keymaps keymaps)
