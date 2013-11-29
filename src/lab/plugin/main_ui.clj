(ns lab.plugin.main-ui
  "DSL to abstract the UIcomponents with Clojure data structures."
  (:require [lab.core :as lab]
            [lab.ui [core :as ui]
                    [select :as ui.sel]
                    [menu :as menu]
                    swing]
            [lab.core [keymap :as km]
                      [plugin :as plugin]]
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

(defn- open-document-ui!
  "Adds a new tab to the documents tab container. This is used by both 
the open and new commands."
  [app doc]
  (as-> (:ui @app) ui
    (ui/update! ui :#documents ui/add (document-tab app doc))))


(defn open-document
  "Adds a new tab with the open document."
  [app path]
  (swap! app lab/open-document path)
  (open-document-ui! app (lab/current-document @app)))

(defn- open-document-menu
  "Opens a file selection dialog for the user to choose a file
and call the app's open-document function."
  [app _]
  (let [file-dialog   (ui/init [:file-dialog {:type :open :visible true}])
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

(defn close-document-button
  [app id & _]
  (let [ui  (:ui @app)
        tab (ui/find @ui (str "#" id))]
    (ui/update! ui :#documents ui/remove tab)))

(defn- close-document
  "Finds the currently selected tab, removes it and closes the document
associated to it."
  [app & _]
  (let [ui     (:ui @app)
        tab    (current-document-tab ui)
        editor (current-text-editor ui)
        doc    (ui/attr editor :doc)]
    (when doc
      (ui/update! ui :#documents ui/remove tab)
      (swap! app lab/close-document doc))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Save

(defn- assign-path
  "When saving, if the document doesn't have a path, get one from the user."
  [doc]
  (if (doc/path doc)
    doc
    (let [file-dialog   (ui/init [:file-dialog {:type :save :visible true}])
          [result file] (ui/attr file-dialog :result)]
      (if file
        (doc/bind doc (.getCanonicalPath file) :new? true)
        doc))))

(defn- save-document
  [app & _]
  (let [ui     (:ui @app)
        editor (current-text-editor ui)
        doc    (ui/attr editor :doc)]
    (swap! doc assign-path)
    (when (doc/path @doc)
      (swap! app lab/save-document doc))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Switch

(defn- switch-document-ui [app evt]
  (let [ui     (:ui @app)
        editor (current-text-editor ui)
        doc    (ui/attr editor :doc)]
    (swap! app lab/switch-document doc)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Insert

(defn text-editor-change [app id doc evt]
  (let [ui     (:ui @app)
        editor (ui/find @ui :text-editor)]
    (case (:type evt)
      :insert (swap! doc doc/insert (:offset evt) (:text evt))
      :remove (swap! doc doc/delete (:offset evt) (+ (:offset evt) (:length evt)))
      :change nil)
    (assert (= (ui/text editor) (doc/text @doc)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Register Keymap

(defn- register-keymap-hook
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

(defn- create-text-editor [app doc]
  (ui/with-id id
  [:text-editor {:doc         doc
                 :text        (doc/text @doc)
                 :border      :none
                 :background  0x333333
                 :foreground  0xFFFFFF
                 :caret-color 0xFFFFFF
                 :key-event   (fn [e super] (println (:event e)) (super))
                 :on-change   (partial #'text-editor-change app id doc)
                 :font        [:name "Monospaced.plain" :size 14]}]))

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
           :scroll    true}
           (create-text-editor app doc)]))

(defn app-window [app]
  [:window {:id     "main"
            :title   (:name @app)
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
              {:category "File" :name "Close" :fn #'close-document :keystroke "ctrl W"}
              {:category "File" :name "Save" :fn #'save-document :keystroke "ctrl S"}
              {:category "View" :name "Fullscreen" :fn #'toggle-fullscreen :keystroke "F4"})])

(defn- init!
  "Builds the basic UI and adds it to the app under the key :ui."
  [app]
  (swap! app assoc :ui (atom (-> app app-window ui/init))))

(plugin/defplugin lab.core.ui
  "Creates the UI for the application and hooks into
basic file operations."
  :init!    #'init!
  :hooks    hooks
  :keymaps  keymaps)