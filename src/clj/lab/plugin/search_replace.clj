(ns lab.plugin.search-replace
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

(defn- goto-line [e]
  (when (= 2 (:click-count e))
    (let [node   (:source e)
          {:keys [editor position]} (ui/attr node :stuff)]
      (ui/caret-position editor position))))

(defn- line-at [editor pos]
  (let [s     (model/text editor)
        start (util/find-char s pos #{\newline} -1)
        end   (util/find-char s pos #{\newline} 1)]
    (subs s (if start start 0) (if end end (dec (count s))))))

(defn- search-results-node [editor [start end]]
  [:tree-node {:item   (line-at editor start)
               :leaf   true
               :stuff  {:position start :editor editor}
               :listen [:click ::goto-line]}])

(defn- add-search-results! [dialog results]
  (let [editor (:editor (ui/attr @dialog :stuff))
        txt    (model/text editor)
        items  (mapv (partial search-results-node editor) results)
        root   (-> [:tree-node {:item :root}] ui/init (ui/add-all items))]
    (ui/update! dialog [:#search-text :tree] #(-> % ui/remove-all (ui/add root)))))

(defn- do-search [hls e]
  (let [dialog  (:dialog (ui/attr (:source e) :stuff))
        editor  (:editor (ui/attr @dialog :stuff))
        ptrn    (model/text (ui/find @dialog :text-field))
        results (when (seq ptrn) (util/find-limits ptrn (model/text editor)))]
    (when results
      (ui/action
        (doseq [hl @hls] (ui/remove-highlight editor hl))
        (reset! hls (mapv (fn [[start end]] (ui/add-highlight editor start end 0x888888))
                          results))
        (add-search-results! dialog results)))))

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
        editor  (current-text-editor @ui)]
    (when editor
      (let [dialog (atom nil)
            hls    (atom nil)
            ch     (search-channel hls)]
        (reset! dialog (-> (tplts/search-text-dialog @ui "Search Text")
                         ui/init
                         (ui/update :#search-text ui/attr :stuff {:chan ch :highlights hls :editor editor})
                         (ui/update :#search-text ui/listen :closing ::close-search-text)
                         (ui/update :#search-text ui/listen :closed ::close-search-text)
                         (ui/update [:#search-text :button] ui/attr :stuff {:dialog dialog})
                         (ui/update [:#search-text :button] ui/listen :click ch)))
        (ui/attr @dialog :visible true)))))

(def ^:private keymaps
  [(km/keymap 'lab.plugin.search-replace
     :global
     {:category "Edit" :fn ::search-text-in-editor :keystroke "ctrl f" :name "Find & Replace"})])

(plugin/defplugin lab.plugin.search-replace
  :type    :global
  :keymaps keymaps)
