(ns lab.plugin.ui
  "DSL to abstract the UIcomponents with Clojure data structures."
  (:require [lab.core :as lab]
            [lab.ui [core :as ui]
                    [select :as ui.sel]
                    [tree :as tree]
                    [menu :as menu]
                    [stylesheet :as css]
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

; Open

(defn- open-document-ui!
  "Adds a new tab to the documents tab container. This is used by both 
the open and new commands."
  [app doc]
  (as-> (:ui @app) ui
    (ui/update! ui :#documents ui/add (document-tab app doc))))


(defn- open-document
  "Adds a new tab with the open document."
  [app path]
  (swap! app lab/open-document path)
  (open-document-ui! app (lab/current-document @app)))

(defn- open-document-menu
  "Opens a file selection dialog for the user to choose a file
and call the app's open-document function."
  [app _]
  (let [file-dialog   (ui/init [:file-dialog {:type :open :visible true}])
        [result file] (ui/get-attr file-dialog :result)]
    (if file
      (open-document app (.getCanonicalPath file))
      app)))

; New

(defn- new-document
  "Creates a new document and shows it in a new tab."
  [app & _]
  (swap! app lab/new-document)
  (open-document-ui! app (lab/current-document @app)))

; Close

(defn close-document-button
  [app id & _]
  (let [ui  (:ui app)
        tab (ui/find @ui (str "#" id))]
    (ui/update! ui :#documents ui/remove tab)))

(defn- close-document
  "Finds the currently selected tab, removes it and closes the document
associated to it."
  [app & _]
  (let [ui     (:ui @app)
        tab    (current-document-tab ui)
        editor (current-text-editor ui)
        doc    (ui/get-attr editor :doc)]
    (when doc
      (ui/update! ui :#documents ui/remove tab)
      (swap! app lab/close-document doc))))

; Save

(defn- assign-path
  "When saving, if the document doesn't have a path, get one from the user."
  [doc]
  (if (doc/path doc)
    doc
    (let [file-dialog   (ui/init [:file-dialog {:type :save :visible true}])
          [result file] (ui/get-attr file-dialog :result)]
      (if file
        (doc/bind doc (.getCanonicalPath file) :new? true)
        doc))))

(defn- save-document
  [app & _]
  (let [ui     (:ui app)
        editor (current-text-editor ui)
        doc    (ui/get-attr editor :doc)]
    (swap! doc assign-path)
    (when (doc/path @doc)
      (swap! app lab/save-document doc))))

; Switch document

(defn- switch-document-ui [app evt]
  (let [ui     (:ui @app)
        editor (current-text-editor ui)
        doc    (ui/get-attr editor :doc)]
    (swap! app lab/switch-document doc)))

; Insert

(defn text-editor-change [app doc evt]
  (case (:type evt)
    :insert (swap! doc doc/insert (:offset evt) (:text evt))
    :remove (swap! doc doc/delete (:offset evt) (+ (:offset evt) (:length evt)))
    :change nil))

; Register

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

(defn- create-text-editor [app doc]
  [:text-editor {:doc         doc
                 :text        (doc/text @doc)
                 :border      :none
                 :background  0x666666
                 :foreground  0xFFFFFF
                 :caret-color 0xFFFFFF
                 :on-change   (partial #'text-editor-change app doc)
                 :font        [:name "Monospaced.plain" :size 14]}])

(defn- document-tab [app doc]
  (ui/with-id id
    [:tab {:tool-tip (doc/path @doc)
           :header   [:panel {:transparent true}
                             [:label {:text (doc/name @doc)}]
                             [:button {:icon         "close-tab.png"
                                       :border       :none
                                       :transparent  true
                                       :on-click     (partial #'close-document-button app id)}]]
           :border    :none}
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
                                             :on-tab-change (partial #'switch-document-ui app)
                                             }]
                                     [:tabs {:id "right-controls"}]]]
                    [:tabs {:id "bottom-controls"}]]])

(defn- toggle-fullscreen
  "Toggles between fullscreen and non fullscreen mode."
  [app _]
  (let [ui    (:ui app)
        full? (-> (ui/find @ui :#main) (ui/get-attr :fullscreen))]
    (ui/update! ui :#main ui/set-attr :fullscreen (not full?)))
  app)

(defn- open-document-tree
  "Handler for the click event of an item in the tree."
  [app {:keys [source click-count]}]
  (when (= click-count 2)
    (let [^java.io.File file (ui/selected source)]
      (when-not (.isDirectory file)
        (open-document app (.getCanonicalPath file))))))

(defn- file-tree [app]
  [:tab {:title "Files" :border :none}
        [:tree {:id      "file-tree"
                :on-click (partial #'open-document-tree app)
                :root     (tree/load-dir "/home/jfacorro/dev/clojure-lab/src/lab/ui/swing")}]])

(defn add-component
  "Add component to a tabs control."
  [app selector title component]
  (let [tab      [:tab {:title title :border :none} component]]
    (ui/update! (app :ui) selector ui/add component)))

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
  (swap! app assoc :ui (atom (-> app app-window ui/init)))
  (add-component @app :#left-controls "Files" (file-tree app)))

(plugin/defplugin lab.core.ui
  "Creates the UI for the application and hooks into
basic file operations."
  :init!    #'init!
  :hooks    hooks
  :keymaps  keymaps)

(comment

(do
  (def ui
    (let [app (init {:name "Clojure Lab - UI dummy"})
          ui  (app :ui)]
      ui))
  (def stylesheet {:split {:border :none}
                   :tabs  {:border :none}
                   :text-editor {:background 0x555555
                                 :font       [:name "Monospaced.plain" :size 14]}})
  (css/apply-stylesheet x stylesheet)
  nil)

)
