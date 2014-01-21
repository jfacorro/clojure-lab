(ns lab.plugin.main-ui
  "Builds the main UI window and components."
  (:require [clojure.core.async :as async]
            [lab.core :as lab]
            [lab.ui [core :as ui]
                    [select :as ui.sel]
                    [menu :as menu]
                    [templates :as tplts]
                    swing]
            [lab.core [keymap :as km]
                      [plugin :as plugin]
                      [lang :as lang]]
            [lab.model.document :as doc]))

(declare document-tab)

(defn- current-document-tab [ui]
  "Returns the currently selected document tab."
  (-> ui
    (ui/find :#center)
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
        id  (ui/attr tab :id)
        editor-id (-> tab (ui/find :text-editor) (ui/attr :id))]
    (add-watch doc (str :editor id) (partial #'doc-modified-update-title app id))
    (ui/action
      (ui/update! ui :#center ui/add tab)
      (ui/update! ui (ui/selector# editor-id) ui/focus))))

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

(declare save-document-ui!)

(defn- save-changes-before-closing
  "Asks the user for confirmation on whether to save a
document before closing. Returns true if the document
should be closed and false otherwise."
  [app tab doc]
  (if-not (doc/modified? @doc)
    true
    (let [dialog (ui/init (tplts/confirm "Save changes"
                                       "Do you want to save the changes made to this file before closing?"))
          result (ui/attr dialog :result)]
      (when (= result :ok)
        (save-document-ui! app tab))
      (not (#{:cancel :closed} result)))))

(defn close-document-ui
  [app id]
  (let [ui     (:ui @app)
        tab    (ui/find @ui (ui/selector# id))
        editor (ui/find tab :text-editor)
        doc    (ui/attr editor :doc)
        close? (save-changes-before-closing app tab doc)
        ;; Get the tab component in case the saving modified it
        ;; TODO: maybe modify the remove so that it takes an id instead
        tab    (ui/find @ui (ui/selector# id))]
    (when close?
      (ui/update! ui :#center ui/remove tab)
      (swap! app lab/close-document doc))))

(defn close-document-button
  "Handles the tabs' close button when clicked."
  [app e]
  (close-document-ui app (-> (:source e) (ui/attr :stuff) :tab-id)))

(defn- close-document-menu
  "Finds the currently selected tab, removes it and closes the document
associated to it."
  [app & _]
  (let [ui     (:ui @app)
        tab    (current-document-tab @ui)
        id     (ui/attr tab :id)]
    (when tab
      (close-document-ui app id))))

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

(defn- save-document-ui! [app tab]
  (let [ui      (:ui @app)
        tab-id  (ui/attr tab :id)
        doc     (-> tab (ui/find :text-editor) (ui/attr :doc))
        cur-dir (lab/config @app :current-dir)]
    (swap! doc assign-path cur-dir)
    (when (doc/path @doc)
        (ui/update! ui (ui/selector# tab-id)
                    update-tab-title (doc/name @doc))
        (swap! app lab/save-document doc))))

(defn- save-document-menu
  [app & _]
  (let [ui     (:ui @app)
        tab    (current-document-tab @ui)]
    (when tab
      (save-document-ui! app tab))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Switch

(defn- switch-document-ui!
  [app evt]
  (let [ui     (:ui @app)
        editor (current-text-editor @ui)
        doc    (ui/attr editor :doc)]
    (when doc
      (swap! app lab/switch-document doc))))

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
                (async/thread (apply f args))
                (recur :wait nil))))))
    c))

(defn highlight
  "Takes the app atom, the id for the current text 
editor control and generates the parse tree. It then
applies all the styles found in the document's language
to the new tokens identified in the last parse tree
generation."
  [editor & [incremental]]
  (let [doc         (ui/attr editor :doc)
        node-group  (and incremental (gensym "group-"))
        lang        (doc/lang @doc)
        styles      (:styles lang)
        parse-tree  (lang/parse-tree @doc node-group)
        old-text    (doc/text editor)]
    (let [tokens (lang/tokens parse-tree node-group)]
      (ui/action
          (when (= (doc/text editor) old-text)
            (ui/apply-style editor tokens styles)))))
  editor)

(defn highlight-by-id
  [app id]
  (let [ui          (:ui @app)
        editor      (ui/find @ui (ui/selector# id))]
    (highlight editor true)))

(defn text-editor-change
  "Handles changes in the control, updates the document
and signals the highlighting process."
  [app {:keys [type source offset text length] :as e}]
  (when (not= type :change)
    (let [ui       (:ui @app)
          id       (ui/attr source :id)
          editor   (ui/find @ui (ui/selector# id))
          channel  (:chan (ui/attr editor :stuff))
          doc      (ui/attr editor :doc)]
      (when editor 
        (when (not (:read-only @doc))
          (case type
            :insert (swap! doc doc/insert offset text)
            :remove (swap! doc doc/delete offset (+ offset length))))
        (async/put! channel [app id])))))

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
        (ui/update! ui [] (partial reduce menu/add-option) cmds))
     :lang  nil
     :local nil)
  (f app keymap))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Controls

(defn- text-editor-post-init [app e]
  (let [c   (:source e)
        doc (ui/attr c :doc)]
    (-> c
      (ui/attr :text (doc/text @doc))
      highlight
      (ui/attr :caret-position 0))))

(defn handle-key [app e]
  (let [ui   (:ui @app)
        editor (:source e)
        doc  (ui/attr editor :doc)
        ks   (-> [] (into (:modifiers e)) (conj (:description e)))
        kstr (->> ks (map name) (interpose " ") (apply str))
        char-typed (-> e :char str)
        cmd  (->> [(doc/keymap @doc) (-> @doc doc/lang :keymap) (@app :keymap)]
              (map #(or (km/find % kstr) (km/find % char-typed)))
              (drop-while nil?)
              first)]
    (when cmd
      (ui/consume e)
      (when (= :pressed (:event e))
        (ui/handle-event (:fn cmd) e)))))

(defn- text-editor-create [app doc]
  (let [id     (ui/genid)
        ch     (timeout-channel 100 #'highlight-by-id)
        editor (ui/init [:text-editor {:id        id
                                       :doc       doc
                                       :post-init ::text-editor-post-init
                                       :on-key    ::handle-key
                                       :on-change ::text-editor-change
                                       :stuff     {:chan ch}}])]
    [:scroll {:vertical-increment 16
              :border :none
              :margin-control [:line-number {:source editor}]}
      [:panel {:border :none
               :layout :border}
        editor]]))

(defn- document-tab
  "Creates a tab with an editor."
  [app doc]
  (let [id    (ui/genid)
        title (doc/name @doc)
        tool-tip (doc/path @doc)]
    (-> (tplts/tab app)
      (ui/update :tab #(-> % (ui/attr :id id)
                             (ui/attr :tool-tip tool-tip)))
      (ui/update [:panel :label] ui/attr :text title)
      (ui/update [:panel :button] ui/attr :stuff {:tab-id id})
      (ui/update [:panel :button] ui/attr :on-click ::close-document-button)
      (ui/add (text-editor-create app doc))
      (ui/apply-stylesheet (:styles @app)))))

(def ^:private split-style
  {:border :none
   :divider-size 3
   :background 0x666666
   :divider-background 0x999999})

(defn app-window [app]
  [:window {:id     "main"
            :title   (lab/config @app :name)
            :visible true
            :size    [700 500]
            :maximized true
            :icons   ["icon-16.png" "icon-32.png" "icon-64.png"]
            :menu    [:menu-bar]}
    [:split (assoc split-style
             :orientation :vertical
             :resize-weight 1)
      [:split (assoc split-style
               :resize-weight 0
               :divider-location 150)
        [:tabs {:id "left" :border :none}]
        [:split (assoc split-style :resize-weight 1)
          [:tabs {:id "center"
                  :on-tab-change ::switch-document-ui!}]
          [:tabs {:id "right"}]]]
      [:tabs {:id "bottom"}]]])

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
;;; Undo/Redo

(defn undo-redo! [app e f]
  (let [ui     (:ui @app)
        editor (current-text-editor @ui)
        id     (ui/attr editor :id)
        doc    (ui/attr editor :doc)
        hist   (doc/history @doc)]
    (swap! doc f)
    ;; TODO: Fix this abominable scheme for undo/redo
    (swap! doc assoc :read-only true)
    (let [[editor hist] (f editor hist)]
      (ui/update! ui (ui/selector# id) (constantly editor)))
    (swap! doc dissoc :read-only)))

(defn redo! [app e]
  (undo-redo! app e doc/redo))

(defn undo! [app e]
  (undo-redo! app e doc/undo))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Event handler

(defn keyword->fn [k]
  (intern (symbol (namespace k)) (symbol (name k))))

(defn event-handler
  "Replaces the UI's default event-handler implementation, 
inserting a fixed first parameter."
  [app f e]
  (cond
    (or (fn? f) (var? f))
      (f app e)
    (keyword? f)
      ((keyword->fn f) app e)
    :else
      (throw (Exception. "Not supported event handler value, it must be a function or a ns qualified keyword."))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Default styles

(def styles
  {:*           {:font [:name "Consolas" :size 12]}
   :line-number {:font        [:name "Consolas" :size 14]
                 :background  0x666666
                 :color       0xFFFFFF
                 :curren-line-color 0x00FFFF}
   :text-editor {:border      :none
                 :font        [:name "Consolas" :size 14]
                 :background  0x333333
                 :color       0xFFFFFF
                 :caret-color 0xFFFFFF}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Plugin definition

(def ^:private hooks
  {#'lab.core.plugin/register-keymap! #'register-keymap-hook})

(def ^:private keymaps
  [(km/keymap (ns-name *ns*)
              :global
              {:category "File", :name "New", :fn ::new-document, :keystroke "ctrl n"}
              {:category "File", :name "Open", :fn ::open-document-menu, :keystroke "ctrl o"}
              {:category "File", :name "Close", :fn ::close-document-menu, :keystroke "ctrl w"}
              {:category "File", :name "Save", :fn ::save-document-menu, :keystroke "ctrl s"}
              {:category "View", :name "Fullscreen", :fn ::toggle-fullscreen, :keystroke "f4"}
              {:category "Edit", :name "Undo", :fn ::undo!, :keystroke "ctrl z"}
              {:category "Edit", :name "Redo", :fn ::redo!, :keystroke "ctrl y"})])

(defn- init!
  "Builds the basic UI and adds it to the app under the key :ui."
  [app]
  (ui/register-event-handler! (partial #'event-handler app))
  (swap! app assoc :ui (atom (ui/init (app-window app)))
                   :styles styles))

(plugin/defplugin lab.plugin.main-ui
  "Creates the UI for the application and hooks into
basic file operations."
  :init!    #'init!
  :hooks    hooks
  :keymaps  keymaps)
