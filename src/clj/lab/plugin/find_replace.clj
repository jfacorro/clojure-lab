(ns lab.plugin.find-replace
  (:require [clojure.java.io :as io]
            [clojure.core.async :as async]
            [lab.core :as lab]
            [lab.core [plugin :as plugin]
                      [keymap :as km]
                      [main :refer [current-text-editor open-document]]]
            [lab.model.protocols :as model]
            [lab.util :refer [find-char find-limits]]
            [lab.ui [core :as ui]
                    [templates :as tplts]])
  (:import  [java.io File]))

(declare find-next-click)
;;;;;;;;;;;;;;;;;;;;;;
;; Views

(defn- view-find [owner dialog]
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

(defn- view-replace [owner dialog]
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
     [:panel {:layout [:box :y] :padding 3 :width 100}
      find-next-btn
      [:button {:text "Replace"
                :listen [:click ::replace-click]
                :stuff {:dialog dialog}}]
      [:button {:text "Replace All"
                :listen [:click ::replace-all-click]
                :stuff {:dialog dialog}}]]]]))

(defn- view-find-in-files [owner dialog]
  (let [find-all-btn  (ui/init [:button {:text "Find All"
                                         :listen [:click ::find-in-files-click]
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
       [:label {:text "Find " :width 40}]
       [:text-field {:id "find-text"
                     :border [:line 0xAAAAAA 1]
                     :padding 0}]]
      [:panel {:layout [:box :line] :padding 5}
       [:label {:text "Path " :width 40}]
       [:text-field {:id "path-text"
                     :border [:line 0xAAAAAA 1]
                     :padding 0
                     :read-only true}]
       [:panel {:width 5}]
       [:button {:text   "Browse..."
                 :listen [:click ::browse-directory]
                 :stuff  {:dialog dialog}}]]
      [:panel {:layout [:box :line] :padding 5}
       [:checkbox {:id "recursive"
                   :text "Recursive"}]
       [:panel]
       find-all-btn]]]))

(defn- view-find-results [app]
  (-> (tplts/tab "find-results")
    (ui/update-attr :header ui/update :label ui/attr :text "Search Results")
    (ui/add [:scroll
             [:tree {:border    :none
                     :hide-root true}
              [:tree-node {:id "find-results-root"}]]])
    (ui/apply-stylesheet (:styles @app))))

;;;;;;;;;;;;;;;;;;;;;;
;; Find in Files

(defn- open-result
  [{:keys [click-count button source app] :as e}]
  (when (and (= 2 click-count) (= :button-1 button))
    (let [node   source
          ui     (:ui @app)
          {:keys [file position]}
                 (ui/stuff node)
          path   (.getCanonicalPath ^File file)]
      (open-document app path)
      (ui/action (ui/caret-position (current-text-editor @ui) position)))))

(defn- line-at [s pos]
  (let [start (find-char s pos #{\newline} dec)
        end   (find-char s pos #{\newline} inc)]
    (subs s (if start start 0) (if end end (count s)))))

(defn- search-results-node [file txt [start end]]
  [:tree-node {:item   (line-at txt start)
               :leaf   true
               :stuff  {:position start :file file}
               :listen [:click ::open-result]}])

(defn- find-results [ui id txt ^File file]
  (let [s       (slurp file)
        results (find-limits txt s)
        items   (map (partial search-results-node file s) results)
        file-node (into [:tree-node {:item (.getCanonicalPath file)}] items)]
  (when (seq items)
    (ui/action (ui/update! ui (ui/id= id) ui/add file-node)))))

(defn- get-files
  [path & [recursive]]
  (let [x (io/file path)]
    (if recursive
      (file-seq x)
      (.listFiles x))))

(defn- find-in-files [app path recursive txt]
  (let [ui    (:ui @app)
        files (get-files path recursive)
        title (str "Find \"" txt "\" in \"" path "\"")
        root  (ui/init [:tree-node {:item title}])
        id    (ui/attr root :id)]
    (ui/action
      (ui/update! ui [:#find-results :#find-results-root] ui/add root))
    (doseq [^File file files]
      (when (.isFile file)
        (find-results ui id txt file)))))

(defn- browse-directory
  "Opens a browse directory dialog and assigns the selected
directory to the text field that holds the path."
  [{:keys [app source] :as e}]
  (let [ui        (:ui @app)
        dialog    (:dialog (ui/stuff source))
        curr-dir  (lab/config @app :current-dir)
        dir-dlg   (ui/init (tplts/directory-dialog "Browse for dir..." curr-dir @ui))
        [res dir] (ui/attr dir-dlg :result)
        path      (.getCanonicalPath ^File dir)]
    (ui/update! dialog :#path-text ui/attr :text path)))

(defn- find-in-files-click
  [{:keys [app source] :as e}]
  (let [ui      (:ui @app)
        dialog  (:dialog (ui/stuff source))
        txt     (model/text (ui/find @dialog :#find-text))
        path    (model/text (ui/find @dialog :#path-text))
        recursive?  (ui/selection (ui/find @dialog :#recursive))]
    (when (and (seq txt) (seq path))
      (ui/action
        (ui/attr @dialog :visible false)
        (when-not (ui/find @ui :#find-results)
          (ui/update! ui :#bottom ui/add (view-find-results app)))
        (future
          (find-in-files app path recursive? txt))))))

;;;;;;;;;;;;;;;;;;;;;;
;; Replace

(defn- replace-click
  "Finds the next match and if the replacement text is not
emtpy"
  [{:keys [app source] :as e}]
  (let [ui     (:ui @app)
        dialog (:dialog (ui/stuff source))
        editor (current-text-editor @ui)
        src    (model/text (ui/find @dialog :#find-text))
        rpl    (model/text (ui/find @dialog :#replace-text))
        [s e]  (and (seq src)
                    (find-next-click e)
                    (ui/selection editor))]
    (when (and (not= s e)
               (seq src))
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
          result  (when (seq ptrn)
                        (first (find-limits ptrn (subs (model/text editor) offset))))]
    (if result
      (boolean (ui/selection editor (map (partial + offset) result)))
      (when (seq (find-limits ptrn (model/text editor)))
          (ui/caret-position editor 0)
          (find-next e))))))

(defn- find-next-click
  "Registers the find pattern in the app and uses the find-next
function to look for the next match."
  [{:keys [app source] :as e}]
  (let [dialog  (:dialog (ui/stuff source))
        ptrn    (model/text (ui/find @dialog :#find-text))]
    (when (seq ptrn)
      (swap! app assoc ::find-pattern ptrn)
      (find-next e))))

;;;;;;;;;;;;;;;;;;;;;;
;; Show dialogs

(defn- show-dialog
  "Show the find and replace dialog, selecting the tab
with the id specified."
  [{:keys [app] :as e} view & [no-editor-required]]
  (let [ui      (:ui @app)
        editor  (current-text-editor @ui)
        dialog  (atom nil)]
    (when (or no-editor-required editor)
      (-> dialog
        (reset! (ui/init (view @ui dialog)))
        (ui/attr :visible true)))))

(defn- show-find
  [e]
  (show-dialog e view-find))

(defn- show-replace
  [e]
  (show-dialog e view-replace))

(defn- show-find-in-files 
  [e]
  (show-dialog e view-find-in-files true))

(def ^:private keymaps
  [(km/keymap "Find & Replace"
     :global
     {:category "Edit" :fn ::show-find :keystroke "ctrl f" :name "Find"}
     {:category "Edit" :fn ::show-find-in-files :keystroke "alt f" :name "Find in Files"}
     {:category "Edit" :fn ::find-next :keystroke "f3" :name "Find Next"}
     {:category "Edit" :fn ::show-replace :keystroke "ctrl h" :name "Replace"})])

(plugin/defplugin lab.plugin.find-replace
  :type    :global
  :keymaps keymaps)
