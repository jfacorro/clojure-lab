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

(defn view [owner dialog]
  (let [find-next-btn  (ui/init [:button {:text "Find Next"
                                     :listen [:click ::find-next-click]
                                     :stuff {:dialog dialog}}])]
    [:dialog {:id "find-replace-dialog"
              :title "Find Text"
              :size  [500 300]
              :modal false
              :owner owner
              :default-button find-next-btn
              :stuff  {:highlights (atom nil)}
              :listen [:closing ::close-find-replace
                       :closed  ::close-find-replace]}
      [:tabs {:selected-tab-style   {:border [:line 0xAAAAAA 1]}
              :unselected-tab-style {:border :none}}
        [:tab {:id "find-tab"
               :layout [:box :page]
               :header [:panel [:label {:text "Find"}]]}
          [:panel {:layout [:box :line] :padding 5}
            [:label {:text "Find "}]
            [:text-field {:border [:line 0xAAAAAA 1]
                          :padding 0
                          :size [200 20]}]]
          [:panel {:layout [:box :line] :padding 5}
              [:panel]
              find-next-btn]]
        [:tab {:id "replace-tab"
               :layout [:box :page]
               :header [:panel [:label {:text "Replace"}]]}
          [:panel {:layout [:box :line] :padding 5}
            [:label {:text "Find " :width 60}]
            [:text-field {:border [:line 0xAAAAAA 1]
                          :padding 0}]]
          [:panel {:layout [:box :line] :padding 5}
            [:label {:text "Replace " :width 60}]
            [:text-field {:border [:line 0xAAAAAA 1]
                          :padding 0}]]
          [:panel {:layout [:box :line] :padding 5}
              [:panel]
              [:button {:text "Find Next"}]
              [:button {:text "Replace"}]
              [:button {:text "Find & Replace"}]]]
        [:tab {:id "find-in-files-tab"
               :layout [:box :page]
               :header [:panel [:label {:text "Find in Files"}]]}]]]))

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

(defn- find-in-files
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

(defn- find-next
  "Checks if there's a find-pattern registered in the app and
finds the next match in the text of the current editor, starting
from the current position of the caret."
  [{:keys [app] :as e}]
  (when (::find-pattern @app)
    (let [ui      (:ui @app)
          editor  (current-text-editor @ui)
          offset  (ui/caret-position editor)
          ptrn    (::find-pattern @app)
          result  (when (seq ptrn) (first (util/find-limits ptrn (subs (model/text editor) offset))))]
    (when result
      (ui/action (ui/selection editor (map (partial + offset) result)))))))

(defn- find-next-click
  "Registers the find pattern in the app and uses the find-next
function to look for the next match."
  [{:keys [app source] :as e}]
  (let [dialog  (:dialog (ui/attr source :stuff))
        ptrn    (model/text (ui/find @dialog :text-field))]
    (swap! app assoc ::find-pattern ptrn)
    (find-next e)))

(defn- close-find-replace
  "Removes all highlights from the editor."
  [e]
  (let [{:keys [editor highlights]} (-> (:source e) (ui/attr :stuff))]
    (ui/action (doseq [hl @highlights] (ui/remove-highlight editor hl)))))

(defn- select-tab [dialog tab-id]
  (ui/update dialog :tabs
             #(ui/selection % (util/index-of (ui/children %)
                                             (ui/find % tab-id)))))

(defn- show-dialog
  "Show the find and replace dialog, selecting the tab
with the id specified."
  [{:keys [app] :as e} tab-id]
  (let [ui      (:ui @app)
        editor  (current-text-editor @ui)
        dialog  (atom nil)]
    (when editor
      (-> dialog 
        (reset! (-> (view @ui dialog)
                    ui/init
                    (select-tab tab-id)))
        (ui/attr :visible true)))))

(defn- show-find
  [e]
  (show-dialog e :#find-tab))

(defn- show-replace
  [e]
  (show-dialog e :#replace-tab))

(defn- show-find-in-files 
  [e]
  (show-dialog e :#find-in-files-tab))

(def ^:private keymaps
  [(km/keymap 'lab.plugin.find-replace
     :global
     {:category "Edit" :fn ::show-find :keystroke "ctrl f" :name "Find"}
     {:category "Edit" :fn ::show-find-in-files :keystroke "alt f" :name "Find in Files"}
     {:category "Edit" :fn ::find-next :keystroke "f3" :name "Find Next"}
     {:category "Edit" :fn ::show-replace :keystroke "ctrl h" :name "Replace"})])

(plugin/defplugin lab.plugin.find-replace
  :type    :global
  :keymaps keymaps)
