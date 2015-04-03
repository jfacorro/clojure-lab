(ns lab.core.main
  "Builds the main UI window and components."
  (:require [clojure.core.async :as async]
            [lab.core :as lab]
            [lab.core [keymap :as km]
                      [plugin :as plugin]
                      [lang :as lang]]
            [lab.model [document :as doc]
                       [protocols :as model]]
            [lab.util :as util]
            [lab.ui [core :as ui]
                    [menu :as menu]
                    [templates :as tplts]]
            lab.ui.swing.core))

(declare document-tab)

(defn- current-editor-tab
  "Returns the currently selected document tab."
  [ui]
  (->> (ui/find ui :#center)
    ui/selection
    ui/id=
    (ui/find ui)))

(defn current-text-editor
  "Returns the currently selected text-editor."
  [ui]
  (ui/find (current-editor-tab ui) :text-editor))

(defn- update-tab-title [tab title]
  (ui/update tab []
             ui/update-attr :header
             ui/update :label ui/attr :text title))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Open

(defn- doc-modified-update-title
  [app id key doc old-state new-state]
  (when (not= (:modified? old-state) (:modified? new-state))
    (let [ui    (:ui @app)
          name  (:name new-state)
          title (if (:modified? new-state) (str name "*") name)]
      (ui/update! ui (ui/id= id) update-tab-title title))))

(defn- has-doc? [doc editor]
  (= doc (ui/attr editor :doc)))

(defn- open-document-ui!
  "Adds a new tab to the documents tab container. This is used by both 
the open and new commands.
If the document is already open, then the text editor associated with
it is brought into focus and the tab containing it is selected."
  [app doc]
  (let [ui     (:ui @app)
        editor (ui/find @ui [:#center [:text-editor (partial has-doc? doc)]])
        id     (ui/attr editor :id)
        tab-id (-> (ui/find @ui [:#center [:tab (ui/child id)]]) (ui/attr :id))]
    (if editor
      (do
        (ui/update! ui :#center tplts/select-tab tab-id)
        (ui/update! ui (ui/id= id) ui/focus))
      (let [tab    (document-tab app doc)
            tab-id (ui/attr tab :id)
            editor (ui/find tab :text-editor)
            id     (ui/attr editor :id)]
        (add-watch doc id (partial #'doc-modified-update-title app tab-id))
        
        (ui/update! ui :#center ui/add tab)
        (ui/update! ui (ui/id= id) ui/focus)
        (lab/load-lang-plugins! app doc)))))

(defn open-document
  "Adds a new tab with the open document."
  [app path]
  (swap! app lab/open-document path)
  (open-document-ui! app (lab/current-document @app)))

(defn- open-document-menu
  "Opens a file selection dialog for the user to choose a file
and call the app's open-document function."
  [e]
  (let [app           (:app e)
        curr-dir      (lab/config @app :current-dir)
        file-dialog   (ui/init (tplts/open-file-dialog curr-dir @(:ui @app)))
        [result file] (ui/attr file-dialog :result)]
    (when (= result :accept)
      (open-document app (.getCanonicalPath ^java.io.File file)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; New

(defn- new-document
  "Creates a new document and shows it in a new tab."
  {:order 1}
  [{app :app :as e}]
  (swap! app lab/new-document)
  (open-document-ui! app (lab/current-document @app)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Close

(declare save-document-ui!)

(defn- save-changes-before-closing
  "Asks the user for confirmation on whether to save a
document before closing. Returns true if the document
should be closed and false otherwise."
  [app tab doc]
  (if-not (:modified? @doc)
    true
    (let [result (tplts/confirm "Save changes"
                                "Do you want to save the changes made to this file before closing?"
                                (-> @app :ui deref))]
      (if (= result :ok)
        (save-document-ui! app tab)
        result))))

(defn close-document-ui
  [app id]
  (let [ui     (:ui @app)
        tab    (ui/find @ui (ui/id= id))
        editor (ui/find tab :text-editor)
        doc    (ui/attr editor :doc)
        result (save-changes-before-closing app tab doc)
        tab    (ui/find @ui (ui/id= id))]
    (when (not (#{:cancel :closed} result))
      (remove-watch doc (ui/attr editor :id))
      (ui/update! ui :#center ui/remove tab)
      (swap! app lab/close-document doc))))

(defn close-document-button
  "Handles the tabs' close button when clicked."
  [{:keys [app source] :as e}]
  (close-document-ui app (:tab-id (ui/stuff source))))

(defn- close-document-menu
  "Finds the currently selected tab, removes it and closes the document
associated to it."
  [e]
  (let [app    (:app e)
        ui     (:ui @app)
        tab    (current-editor-tab @ui)
        id     (ui/attr tab :id)]
    (when tab
      (close-document-ui app id))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Save

(defn- assign-path!
  "When saving, if the document doesn't have a path, get one from the user."
  [app doc current-dir]
  (if (:path @doc)
    doc
    (let [ui            (:ui @app)
          file-dialog   (ui/init (tplts/save-file-dialog current-dir @ui))
          [result file] (ui/attr file-dialog :result)]
      (when (= result :accept)
        (swap! doc doc/bind (.getCanonicalPath ^java.io.File file) :new? true))
      result)))

(defn- save-document-ui! [app tab]
  (let [ui      (:ui @app)
        tab-id  (ui/attr tab :id)
        doc     (-> tab (ui/find :text-editor) (ui/attr :doc))
        cur-dir (lab/config @app :current-dir)
        result  (assign-path! app doc cur-dir)]
    (when (:path @doc)
      (ui/update! ui (ui/id= tab-id)
        update-tab-title (:name @doc))
      (swap! app lab/save-document doc)
      (lab/load-lang-plugins! app doc))
    result))

(defn- save-document-menu
  [e]
  (let [app    (:app e)
        ui     (:ui @app)
        tab    (current-editor-tab @ui)]
    (when tab
      (save-document-ui! app tab))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Switch

(defn- switch-document-ui!
  [e]
  (let [app    (:app e)
        ui     (:ui @app)
        editor (current-text-editor @ui)
        doc    (ui/attr editor :doc)]
    (when doc
      (swap! app lab/switch-document doc))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Register Keymap

(defn- command-order
  "Takes a command specified in a global keymap and extracts
  the :order metadata associated with the fn's var."
  [{:keys [fn]}]
  (let [fn-var (if (keyword? fn)
                 (util/memoized-kw->fn fn)
                 fn)]
    (or (-> fn-var meta :order)
      Integer/MAX_VALUE)))

(defn- register-keymap-hook
  "Processes the :global keymaps adding their commands 
  to the UI's main menu."
  [f app keymap]
  (case (:type keymap)
    :global
      (let [ui    (:ui @app)
            cmds  (->> keymap :bindings vals 
                    (sort-by command-order))]
        (ui/update! ui [] (partial reduce menu/add-option) cmds))
    :lang  nil
    :local nil)
  (f app keymap))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Text Editor 

;; Text Change

(defn text-editor-change
  "Handles changes in the control, updates the document
and signals the highlighting process."
  [{:keys [app type source offset text length] :as e}]
  (let [editor   source
        doc      (ui/attr editor :doc)]
    (case type
      :insert (swap! doc doc/insert offset text)
      :remove (swap! doc doc/delete offset (+ offset length)))))

;; Key handle

;; Change font size

(defn change-font-size [e]
  (when (contains? (:modifiers e) :meta)
    (let [app    (:app e)
          ui     (:ui @app)
          id     (-> (ui/find (:source e) :text-editor) (ui/attr :id))
          editor (ui/find @ui (ui/id= id))
          op     (if (neg? (:wheel-rotation e)) inc dec)
          font   (-> (apply hash-map (ui/attr editor :font))
                   (update-in [:size] op)
                   seq flatten vec)]
      (ui/consume e)
      (ui/update! ui (ui/id= id) ui/attr :font font))))

;; Text editor creation

(defn- line-number-create
  [app editor]
  (-> [:line-number {:source editor :update-font true}]
    ui/init
    (ui/apply-stylesheet (:styles @app))))

(defn- editor-keymap
  [{:keys [keymap lang] :as doc}]
  (or (km/append (:keymap lang) keymap) {}))

(defn- handle-key
  [{app :app :as e}]
  (when-not (ui/consumed? e)
    (let [ui     (:ui @app)
          editor (:source e)
          doc    (ui/attr editor :doc)
          [x y]  (ui/key-stroke (dissoc e :source))
          cmd    (->> [(:keymap @doc)
                       (-> @doc :lang :keymap)
                     (@app :keymap)]
                   (map #(km/find-or % y x))
                   (drop-while nil?)
                   first)]
      (when cmd
        (ui/consume e)
        (when (= :pressed (:event e))
          (ui/handle-event (:fn cmd) e))))))

(defn text-editor-view
  "Creates a text editor with a document attached to it."
  [doc]
  (-> [:text-editor {:doc doc
                     :text (doc/text @doc)
                     :listen [:key #'handle-key             ;; FIXME: This should be using a keymap
                                                            ;; using the (editor-keymap @doc), but lang plugin
                                                            ;; are being loaded after the editor is created.
                              :insert ::text-editor-change
                              :delete ::text-editor-change]}]
      ui/init
      (ui/caret-position 0)))

(defn- text-editor-create [app doc]
  (let [editor (text-editor-view doc)]
    [:scroll {:vertical-increment 16
              :listen [:mouse-wheel ::change-font-size]
              :margin-control (line-number-create app editor)}
      editor]))

;; Document tab creation

(defn- document-tab
  "Creates a tab with an editor."
  [app doc]
  (let [id    (ui/genid)
        title (:name @doc)
        tool-tip (:path @doc)]
    (-> (tplts/tab id)
        (ui/update-attr :stuff assoc :close-tab close-document-button)
        (ui/attr :tool-tip tool-tip)
        (ui/update-attr :header ui/update :label ui/attr :text title)
        (ui/add (text-editor-create app doc))
        (ui/apply-stylesheet (:styles @app)))))

(def ^:private debugging? (= "true" (System/getProperty "clojure.debug")))

(defn- exit!
  "Checks if there are any modified documents before leaving."
  [{:keys [app] :as e}]
  (let [modified? (some (comp :modified? deref) (:documents @app))
        result    (if modified?
                    (tplts/confirm "Bye, but first..."
                      "There are some documents with unsaved changes. Do you want to leave anyway?"
                      (-> @app :ui deref))
                    :ok)]
    (cond
      (and debugging? (= result :ok))
        (do
          (ui/attr @(:ui @app) :visible false)
          (reset! app nil))
      (= result :ok)
        (System/exit 0))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Toogle Fullscreen

(defn- toggle-fullscreen
  "Toggles between fullscreen and non fullscreen mode."
  [e]
  (let [app   (:app e)
        ui    (:ui @app)
        full? (-> (ui/find @ui :#main) (ui/attr :fullscreen))]
    (ui/update! ui :#main ui/attr :fullscreen (not full?))
    app))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Toogle Line Numbers

(defn- toggle-line-numbers
  "Hides and shows the line number for the currently active editor."
  [e]
  (let [app    (:app e)
        ui     (:ui @app)
        tab    (current-editor-tab @ui)
        editor (ui/find tab :text-editor)]
    (when (and tab editor)
      (let [scroll       (ui/find tab :scroll)
            id           (ui/attr scroll :id)
            line-number  (ui/attr scroll :margin-control)]
        (if line-number
          (ui/update! ui (ui/id= id) ui/attr :margin-control nil)
          (ui/update! ui (ui/id= id) ui/attr :margin-control (line-number-create app editor)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Toogle Word Wrap

(defn- toggle-word-wrap
  "Toggles between activating and deactivating word wrap for the
  current text-editor."
  [{:keys [app] :as e}]
  ;; The following implementation is actually sepcific for Swing.
  (let [ui     (:ui @app)
        tab    (current-editor-tab @ui)
        tab-id (ui/attr tab :id)
        scroll (ui/find tab :scroll)
        panel  (ui/find scroll :panel)
        editor (ui/find scroll :text-editor)]
    (ui/update! ui [(ui/id= tab-id) :scroll]
                #(if panel
                   (-> % (ui/remove panel) (ui/add editor))
                   (-> % (ui/remove editor)
                       (ui/add [:panel {:border :none
                                        :layout :border}
                                editor]))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Next/previous center tab

(defn move-tab [e move]
  (let [app   (:app e)
        ui    (:ui @app)
        tab   (current-editor-tab @ui)
        tabs  (ui/find @ui :#center)
        children (ui/children tabs)
        total (count children)
        i     (->> children
                (keep-indexed #(when (= tab %2) %1))
                first
                (move total))]
    (when (pos? total)
      (ui/update! ui :#center ui/selection i))))

(defn next-tab [e]
  (move-tab e
            (fn [total i] (if (and i (< (inc i) total)) (inc i) 0))))

(defn prev-tab [e]
  (move-tab e
            (fn [total i] (if (and i (>= (dec i) 0)) (dec i) (dec total)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Event handler

(defn- handle-keymap
  [km {:keys [app] :as e}]
  (when-not (ui/consumed? e)
    (let [[x y] (ui/key-stroke e)
          km    (km/append (:keymap @app) km)
          cmd   (km/find-or km x y)]
      (when cmd
        (ui/consume e)
        (when (= :pressed (:event e))
          (ui/handle-event (:fn cmd) e))))))

(defn event-handler
  "Replaces the UI's default event-handler implementation, 
inserting a fixed first parameter, which is the app."
  [app f e]
  (let [e (assoc e :app app)]
    (cond
      (or (fn? f) (var? f))
        (f e)
      (keyword? f)
        ((util/memoized-kw->fn f) e)
      (util/channel? f)
        (async/put! f e)
      (map? f)
        (handle-keymap f e)
      :else
        (throw (ex-info "Not supported event handler, it must be a function, an ns qualified keyword or a channel."
                        {:handler f})))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Default styles

(def default-font [:name "Monospaced" :size 15])

(def styles
  [#{:label :tree :button}
                {:font default-font}
   :tree        {:background 0x333333
                 :selected-node-background 0x666666
                 :selected-node-color 0xFFFFFF
                 :unselected-node-background 0x333333
                 :unselected-node-color 0xFFFFFF
                 :open-icon "folder.png"
                 :closed-icon "folder.png"
                 :leaf-icon "file.png"}
   :scroll      {:vertical-increment 16}
   #{:text-editor :text-area :text-field :scroll :split :panel :tree}
                {:border :none
                 :padding 0}
   :line-number {:font        default-font
                 :background  0x222222
                 :color       0xFFFFFF
                 :current-line-color 0x99FFFF
                 :border [:line 0x99FFFF [0 0 0 1]]
                 :padding [0 5]}
   [:pop-up-menu :scroll]
                {:border [:line 0xFFFFFF 1]}
   #{:text-editor :text-area :text-field :console}
                {:font        default-font
                 :background  0x333333
                 :color       0xFFFFFF
                 :caret-color 0xFFFFFF}
   :panel       {:background 0x666666
                 :layout :border}
   :split       {:divider-size 5
                 :background 0x666666
                 :divider-background 0x666666}
   :tabs        {:selected-tab-style   {:border [:line 0x99FFFF [0 0 2 0]]}
                 :unselected-tab-style {:border :none}
                 ; Padding is necessary because :tabs inside
                 ; a :split are drawn with a white line on top
                 :padding [-2 0 0 0]}
   [:#nrepl :split]
                {:divider-size 5
                 :divider-background 0x99FFFF}])

(defn app-view [title]
  (ui/init
    [:window {:id     "main"
              :title   title
              :visible true
              :size    [700 500]
              :maximized true
              :icons   ["icon-16.png" "icon-32.png" "icon-64.png"]
              :menu    [:menu-bar]
              :listen  [:closing ::exit!]}
     [:split {:id "top-bottom"
              :orientation :vertical
              :resize-weight 1}
      [:split {:id "left-center"
               :resize-weight 0
               :divider-location 150}
       [:tabs {:id "left" :border :none}]
       [:split {:id "center-right"
                :resize-weight 1}
        [:tabs {:id "center"
                :listen [:change ::switch-document-ui!]}]
        [:tabs {:id "right"}]]]
      [:tabs {:id "bottom"}]]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Plugin definition

(def ^:private hooks
  {#'lab.core.plugin/register-keymap! #'register-keymap-hook})

(def ^:private keymaps
  [(km/keymap "Basic Commands"
     :global
     {:category "File", :name "New", :fn ::new-document, :keystroke "meta n"}
     {:category "File", :name "Open", :fn ::open-document-menu, :keystroke "meta o"}
     {:category "File", :name "Close", :fn ::close-document-menu, :keystroke "meta w"}
     {:category "File", :name "Save", :fn ::save-document-menu, :keystroke "meta s"}
     {:category "File", :name "Quit", :fn ::exit!, :keystroke "meta q"}
     
     {:category "View", :name "Fullscreen", :fn ::toggle-fullscreen, :keystroke "f4"}
     {:category "View", :name "Show/Hide Line Numbers", :fn ::toggle-line-numbers, :keystroke "meta l"}
     {:category "View", :name "Word Wrap", :fn ::toggle-word-wrap}
     {:category "View", :name "Next tab", :fn ::next-tab, :keystroke "meta tab"}
     {:category "View", :name "Prev tab", :fn ::prev-tab, :keystroke "meta alt tab"})])

(defn- init!
  "Builds the basic UI and adds it to the app under the key :ui."
  [app]
  (ui/register-event-handler! (partial #'event-handler app))
  (swap! app assoc :ui (-> (app-view (lab/config @app :name))
                           (ui/apply-stylesheet styles)
                           atom)
                   :styles styles))

(plugin/defplugin lab.core.main
  "Creates the UI for the application and hooks into
basic file operations."
  :type     :global
  :init!    #'init!
  :hooks    hooks
  :keymaps  keymaps)
