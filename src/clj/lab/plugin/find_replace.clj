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
;; Views

(defn view-find [owner dialog]
  (let [find-next-btn  (ui/init [:button {:text "Find Next"
                                          :listen [:click ::find-next-click]
                                          :stuff {:dialog dialog}}])]
    [:dialog {:id "find-replace-dialog"
              :title "Find Text"
              :size  [300 90]
              :icons ["search-icon.png"]
              :resizable false
              :modal false
              :owner owner
              :default-button find-next-btn}
    [:panel {:layout [:box :page]}
      [:panel {:layout [:box :line] :padding 5}
       [:label {:text "Find "}]
       [:text-field {:id "find-text"
                     :border [:line 0xAAAAAA 1]
                     :padding 0}]]
      [:panel {:layout [:box :line] :padding 2}
       [:panel]
       find-next-btn]]]))

(defn view-replace [owner dialog]
  (let [find-next-btn  (ui/init [:button {:text "Find Next"
                                     :listen [:click ::find-next-click]
                                     :stuff {:dialog dialog}}])]
    [:dialog {:id "find-replace-dialog"
              :title "Replace"
              :size  [310 110]
              :icons ["search-icon.png"]
              :resizable false
              :modal false
              :owner owner
              :default-button find-next-btn}
    [:panel {:layout [:box :line]}
     [:panel {:layout [:box :page]}
      [:panel {:layout [:box :line] :padding 3}
       [:label {:text "Find " :width 60}]
       [:text-field {:id "find-text"
                     :border [:line 0xAAAAAA 1]
                     :padding 0}]]
      [:panel {:layout [:box :line] :padding 3}
       [:label {:text "Replace " :width 60}]
       [:text-field {:id "replace-text"
                     :border [:line 0xAAAAAA 1]
                     :padding 0}]]
      [:panel {:height 10000}]]
     [:panel {:layout [:box :y] :padding 3}
      find-next-btn
      [:button {:text "Replace"
                :listen [:click ::replace-click]
                :stuff {:dialog dialog}}]
      [:button {:text "Replace All"
                :listen [:click ::replace-all-click]
                :stuff {:dialog dialog}}]]]]))

(defn view-find-in-files [owner dialog]
  (let [find-all-btn  (ui/init [:button {:text "Find All"
                                     :listen [:click ::find-next-click]
                                     :stuff {:dialog dialog}}])]
    [:dialog {:id "find-replace-dialog"
              :title "Find in Files"
              :size  [300 130]
              :icons ["search-icon.png"]
              :resizable false
              :modal false
              :owner owner
              :default-button find-all-btn}
    [:panel {:layout [:box :page]}
      [:panel {:layout [:box :line] :padding 5}
       [:label {:text "Find "}]
       [:text-field {:border [:line 0xAAAAAA 1]
                     :padding 0}]]
      [:panel {:layout [:box :line] :padding 5}
       [:label {:text "Path "}]
       [:text-field {:border [:line 0xAAAAAA 1]
                     :padding 0}]
       [:panel {:width 5}]
       [:button {:text "Browse..."}]]
      [:panel {:layout [:box :line] :padding 5}
       [:panel]
       find-all-btn]]]))

;;;;;;;;;;;;;;;;;;;;;;
;; Find in Files

(defn- goto-result [{:keys [click-count button] :as e}]
  (when (and (= 2 click-count) (= :button-1 button))
    (let [node   (:source e)
          {:keys [editor position]} (ui/attr node :stuff)]
      (ui/caret-position editor position))))

(defn- line-at [s pos]
  (let [start (util/find-char s pos #{\newline} -1)
        end   (util/find-char s pos #{\newline} 1)]
    (subs s (if start start 0) (if end end (count s)))))

(defn- search-results-node [editor [start end]]
  [:tree-node {:item   (line-at (model/text editor) start)
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

;;;;;;;;;;;;;;;;;;;;;;
;; Replace

(defn- replace-click
  "Finds the next match and if the replacement text is not
emtpy"
  [{:keys [app source] :as e}]
  (let [ui     (:ui @app)
        dialog (:dialog (ui/attr source :stuff))
        editor (current-text-editor @ui)
        _      (find-next-click e)
        src    (model/text (ui/find @dialog :#find-text))
        rpl    (model/text (ui/find @dialog :#replace-text))
        [s e]  (ui/selection editor)]
    (when (and (not= s e) (seq rpl) (seq src))
      (model/delete editor s e)
      (model/insert editor s rpl)
      (ui/selection editor [s (+ s (count rpl))])
      true)))

(defn- replace-all-click
  [e]
  (when (replace-click e)
    (recur e)))

;;;;;;;;;;;;;;;;;;;;;;
;; Find next

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
      (ui/selection editor (map (partial + offset) result))
      true))))

(defn- find-next-click
  "Registers the find pattern in the app and uses the find-next
function to look for the next match."
  [{:keys [app source] :as e}]
  (let [dialog  (:dialog (ui/attr source :stuff))
        ptrn    (model/text (ui/find @dialog :#find-text))]
    (swap! app assoc ::find-pattern ptrn)
    (find-next e)))

(defn- select-tab [dialog tab-id]
  (ui/update dialog :tabs
             #(ui/selection % (util/index-of (ui/children %)
                                             (ui/find % tab-id)))))

(defn- show-dialog
  "Show the find and replace dialog, selecting the tab
with the id specified."
  [{:keys [app] :as e} view-find]
  (let [ui      (:ui @app)
        editor  (current-text-editor @ui)
        dialog  (atom nil)]
    (when editor
      (-> dialog
        (reset! (ui/init (view-find @ui dialog)))
        (ui/attr :visible true)))))

(defn- show-find
  [e]
  (show-dialog e view-find))

(defn- show-replace
  [e]
  (show-dialog e view-replace))

(defn- show-find-in-files 
  [e]
  (show-dialog e view-find-in-files))

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
