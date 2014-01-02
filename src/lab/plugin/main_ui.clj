(ns lab.plugin.main-ui
  "Builds the main UI window and components."
  (:require [clojure.core.async :as async]
            [lab.core :as lab]
            [lab.ui [core :as ui]
                    [select :as ui.sel]
                    [menu :as menu]
                    [templates :as tpl]
                    swing]
            [lab.core [keymap :as km]
                      [plugin :as plugin]
                      [lang :as lang]]
            [lab.model.document :as doc]))

(declare document-tab)

(defn- current-document-tab [ui]
  "Returns the currently selected document tab."
  (-> @ui
    (ui/find :#documents)
    ui/selected))

(defn- current-text-editor
  "Returns the currently selected text-editor."
  [ui]
  (ui/find (current-document-tab ui) :text-editor))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Open

(defn- update-tab-title [tab title]
  (let [header (-> (ui/attr tab :header)
                  (ui/update :label ui/attr :text title))]
    (ui/attr tab :header header)))

(defn- doc-modified-update-title [app id key doc old-state new-state]
  (when (not= (doc/modified? old-state) (doc/modified? new-state))
    (let [ui    (:ui @app)
          name  (doc/name new-state)
          title (if (doc/modified? new-state) (str name "*") name)]
      (ui/update! ui (ui/selector# id) update-tab-title title))))

(defn- open-document-ui!
  "Adds a new tab to the documents tab container. This is used by both 
the open and new commands."
  [app doc]
  (let [ui  (:ui @app)
        tab (document-tab app doc)
        id  (ui/attr tab :id)]
    (add-watch doc (str :editor id) (partial #'doc-modified-update-title app id))
    (ui/update! ui :#documents ui/add tab)))

(defn open-document
  "Adds a new tab with the open document."
  [app path]
  (swap! app lab/open-document path)
  (open-document-ui! app (lab/current-document @app)))

(defn- open-document-menu
  "Opens a file selection dialog for the user to choose a file
and call the app's open-document function."
  [app _]
  (let [curr-dir      (lab/config @app :current-dir)
        file-dialog   (ui/init [:file-dialog {:type :open
                                              :visible true 
                                              :current-dir curr-dir}])
        [result file] (ui/attr file-dialog :result)]
    (when file
      (open-document app (.getCanonicalPath file)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; New

(defn- new-document
  "Creates a new document and shows it in a new tab."
  [app & _]
  (swap! app lab/new-document)
  (open-document-ui! app (lab/current-document @app)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Close

(defn- confirm-close-doc [doc]
  (if (doc/modified? @doc)
    (let [dialog (ui/init (tpl/confirm "Closing modified file"
                                       "Do you want to close this modified file without saving the changes?"))
          result (ui/attr dialog :result)]
      (= result :ok))
    true))

(defn close-document-ui
  [app id]
  (let [ui     (:ui @app)
        tab    (ui/find @ui (ui/selector# id))
        editor (ui/find tab :text-editor)
        doc    (ui/attr editor :doc)]
    (when (confirm-close-doc doc)
      (ui/update! ui :#documents ui/remove tab)
      (swap! app lab/close-document doc))))

(defn close-document-button
  "Handles the tabs' close button when clicked."
  [app id & _]
  (close-document-ui app id))

(defn- close-document-menu
  "Finds the currently selected tab, removes it and closes the document
associated to it."
  [app & _]
  (let [ui     (:ui @app)
        tab    (current-document-tab ui)
        id     (ui/attr tab :id)]
    (close-document-ui app id)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Save

(defn- assign-path
  "When saving, if the document doesn't have a path, get one from the user."
  [doc current-dir]
  (if (doc/path doc)
    doc
    (let [file-dialog   (ui/init [:file-dialog {:type :save :visible true :current-dir current-dir}])
          [result file] (ui/attr file-dialog :result)]
      (if file
        (doc/bind doc (.getCanonicalPath file) :new? true)
        doc))))

(defn- update-document-tab-title
  "Updated the document tab title."
  [tab title]
  (let [header (-> (ui/attr tab :header)
                   (ui/update [:panel :label] ui/attr :text title))]
    (ui/attr tab :header header)))

(defn- save-document
  [app & _]
  (let [ui     (:ui @app)
        tab    (current-document-tab ui)
        tab-id (ui/attr tab :id)
        editor (ui/find tab :text-editor)
        doc    (ui/attr editor :doc)]
    (swap! doc assign-path (lab/config @app :current-dir))
    (when (doc/path @doc)
      (ui/update! ui (ui/selector# tab-id)
        update-document-tab-title (doc/name @doc))
      (swap! app lab/save-document doc))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Switch

(defn- switch-document-ui [app evt]
  (let [ui     (:ui @app)
        editor (current-text-editor ui)
        doc    (ui/attr editor :doc)]
    (swap! app lab/switch-document doc)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Text Change

(defn timeout-channel
  "Creates a go block that works in two modes :wait and :recieve.
When on ':wait' it blocks execution until a value is recieved
from the channel, it then enters ':recieve' mode until the timeout
wins. Returns a channel that takes the input events."
  [timeout-ms f]
  (let [c (async/chan)]
    (async/go-loop [mode     :wait
                    args     nil]
      (condp = mode
        :wait
          (recur :recieve (async/<! c))
        :recieve
          (let [[_ ch] (async/alts! [c (async/timeout timeout-ms)])]
            (if (= ch c)
              (recur :recieve args)
              (do
                (future (apply f args))
                (recur :wait nil))))))
    c))

(defn highlight
  "Takes the app atom, the id for the current text 
editor control and generates the parse tree. It then
applies all the styles found in the document's language
to the new tokens identified in the last parse tree
generation."
  [app id]
  (let [ui          (:ui @app)
        editor      (ui/find @ui (ui/selector# id))
        doc         (ui/attr editor :doc)
        node-group  (gensym "group-")
        lang        (doc/lang @doc)
        styles      (:styles lang)]
    (swap! doc lang/parse-tree node-group)
    (let [tokens (lang/tokens (doc/parse-tree @doc) node-group)]
      (ui/action (ui/apply-style editor tokens styles)))))

(defn text-editor-change [app editor-id channel evt]
  (when (not= (:type evt) :change)
    (let [ui       (:ui @app)
          editor   (ui/find @ui (ui/selector# editor-id))
          doc      (ui/attr editor :doc)]
      (case (:type evt)
        :insert (swap! doc doc/insert (:offset evt) (:text evt))
        :remove (swap! doc doc/delete (:offset evt) (+ (:offset evt) (:length evt))))
      #_(async/put! channel [app editor-id])
      (assert (= (ui/text editor) (doc/text @doc))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Register Keymap

(defn- register-keymap-hook
  "Processes the :global keymaps adding their commands 
to the UI's main menu."
  [f app keymap]
  (case (:type keymap)
    :global
      (let [ui    (:ui @app)
            cmds  (-> keymap :bindings vals)]
        (ui/update! ui [] (partial reduce (partial menu/add-option app)) cmds))
     :lang  nil
     :local nil)
  (f app keymap))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Controls

(def text-editor-style
  {:border      :none
   :background  0x333333
   :foreground  0xFFFFFF
   :caret-color 0xFFFFFF
   :font        [:name "Consolas" :size 14]})

(defn- create-text-editor [app doc]
  (ui/with-id id
   [:text-editor (merge ;text-editor-style
                        {:doc       doc
                         :on-change (partial #'text-editor-change app id (timeout-channel 250 highlight))
                         :syntax    :clojure})]))

(defn- document-tab [app doc]
  (ui/with-id id
    [:tab {:tool-tip (doc/path @doc)
           :header   [:panel {:transparent true}
                             [:label {:text (doc/name @doc)}]
                             [:button {:icon         "close-tab.png"
                                       :border       :none
                                       :transparent  true
                                       :on-click     (partial #'close-document-button app id)}]]
           :border    :none
           :scroll    false}
           [:scroll-text-editor (create-text-editor app doc)]]))

(defn app-window [app]
  [:window {:id     "main"
            :title   (lab/config @app :name)
            :visible true
            :size    [700 500]
            :maximized true
            :icons   ["icon-16.png" "icon-32.png" "icon-64.png"]
            :menu    [:menu-bar]}
            [:split {:orientation :vertical
                     :resize-weight 1
                     :border :none}
                    [:split {:resize-weight 0
                             :divider-location 150}
                            [:tabs {:id "left-controls"}]
                            [:split {:resize-weight 1}
                                     [:tabs {:id "documents"
                                             :on-tab-change (partial #'switch-document-ui app)}]
                                     [:tabs {:id "right-controls"}]]]
                    [:tabs {:id "bottom-controls"}]]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Toogle Fullscreen

(defn- toggle-fullscreen
  "Toggles between fullscreen and non fullscreen mode."
  [app _]
  (let [ui    (:ui @app)
        full? (-> (ui/find @ui :#main) (ui/attr :fullscreen))]
    (ui/update! ui :#main ui/attr :fullscreen (not full?)))
  app)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Plugin definition

(def ^:private hooks
  {#'lab.core.plugin/register-keymap! #'register-keymap-hook})

(def ^:private keymaps
  [(km/keymap (ns-name *ns*)
              :global
              {:category "File" :name "New" :fn #'new-document :keystroke "ctrl N"}
              {:category "File" :name "Open" :fn #'open-document-menu :keystroke "ctrl O"}
              {:category "File" :name "Close" :fn #'close-document-menu :keystroke "ctrl W"}
              {:category "File" :name "Save" :fn #'save-document :keystroke "ctrl S"}
              {:category "View" :name "Fullscreen" :fn #'toggle-fullscreen :keystroke "F4"})])

(defn- init!
  "Builds the basic UI and adds it to the app under the key :ui."
  [app]
  (swap! app assoc :ui (atom (ui/init (app-window app)))))

(plugin/defplugin lab.plugin.main-ui
  "Creates the UI for the application and hooks into
basic file operations."
  :init!    #'init!
  :hooks    hooks
  :keymaps  keymaps)
