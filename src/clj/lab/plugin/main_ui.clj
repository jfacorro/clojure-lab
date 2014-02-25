(ns lab.plugin.main-ui
  "Builds the main UI window and components."
  (:require [clojure.core.async :as async]
            [lab.core :as lab]
            [lab.util :as util]
            [lab.ui [core :as ui]
                    [select :as ui.sel]
                    [menu :as menu]
                    [templates :as tplts]]
            lab.ui.swing.core
            [lab.core [keymap :as km]
                      [plugin :as plugin]
                      [lang :as lang]]
            [lab.model.document :as doc]
            [lab.model.protocols :as model]))

(declare document-tab)

(defn- current-document-tab [ui]
  "Returns the currently selected document tab."
  (->> (ui/find ui :#center)
    ui/selection
    ui/selector#
    (ui/find ui)))

(defn current-text-editor
  "Returns the currently selected text-editor."
  [ui]
  (ui/find (current-document-tab ui) :text-editor))

(defn- update-tab-title [tab title]
  (let [header (-> (ui/attr tab :header)
                  (ui/update :label ui/attr :text title))]
    (ui/attr tab :header header)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Open

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
        file-dialog   (ui/init (tplts/open-file-dialog curr-dir))
        [result file] (ui/attr file-dialog :result)]
    (when (= result :accept)
      (open-document app (.getCanonicalPath ^java.io.File file)))))

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
    (let [result (tplts/confirm "Save changes"
                                "Do you want to save the changes made to this file before closing?")]
      (if (= result :ok)
        (save-document-ui! app tab)
        result))))

(defn close-document-ui
  [app id]
  (let [ui     (:ui @app)
        tab    (ui/find @ui (ui/selector# id))
        editor (ui/find tab :text-editor)
        doc    (ui/attr editor :doc)
        result (save-changes-before-closing app tab doc)
        tab    (ui/find @ui (ui/selector# id))]
    (when (not (#{:cancel :closed :no} result))
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

(defn- assign-path!
  "When saving, if the document doesn't have a path, get one from the user."
  [doc current-dir]
  (if (doc/path doc)
    doc
    (let [file-dialog   (ui/init (tplts/save-file-dialog current-dir))
          [result file] (ui/attr file-dialog :result)]
      (when (= result :ok)
        (swap! doc doc/bind (.getCanonicalPath ^java.io.File file) :new? true))
      result)))

(defn- save-document-ui! [app tab]
  (let [ui      (:ui @app)
        tab-id  (ui/attr tab :id)
        doc     (-> tab (ui/find :text-editor) (ui/attr :doc))
        cur-dir (lab/config @app :current-dir)
        result  (assign-path! doc cur-dir)]
    (when (doc/path @doc)
        (ui/update! ui (ui/selector# tab-id)
                    update-tab-title (doc/name @doc))
        (swap! app lab/save-document doc))
    result))

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
;;; Text Editor 

;; Text Change

(defn text-editor-change
  "Handles changes in the control, updates the document
and signals the highlighting process."
  [app {:keys [type source offset text length] :as e}]
  (let [editor   source
        doc      (ui/attr editor :doc)]
    (when (not (:read-only @doc))
      (case type
        :insert (swap! doc doc/insert offset text)
        :remove (swap! doc doc/delete offset (+ offset length))))))

;; Key handle

(defn- handle-key [app e]
  (let [ui   (:ui @app)
        editor (:source e)
        doc  (ui/attr editor :doc)
        [x y](ui/key-stroke (dissoc e :source))
        cmd  (->> [(doc/keymap @doc) (-> @doc doc/lang :keymap) (@app :keymap)]
              (map #(km/find-or % y x))
              (drop-while nil?)
              first)]
    (when cmd
      (ui/consume e)
      (when (= :pressed (:event e))
        (ui/handle-event (:fn cmd) e)))))

;; Change font size
(defn change-font-size [app e]
  (when (contains? (:modifiers e) :ctrl)
    (let [ui     (:ui @app)
          id     (-> (ui/find (:source e) :text-editor) (ui/attr :id))
          editor (ui/find @ui (ui/selector# id))
          op     (if (neg? (:wheel-rotation e)) inc dec)
          font   (-> (apply hash-map (ui/attr editor :font))
                   (update-in [:size] op)
                   seq flatten vec)]
      (ui/consume e)
      (ui/update! ui (ui/selector# id) ui/attr :font font))))

;; Text editor creation

(defn- text-editor-create [app doc]
  (let [editor (-> (tplts/text-editor doc)
                 (ui/listen :key ::handle-key)
                 (ui/listen :insert ::text-editor-change)
                 (ui/listen :delete ::text-editor-change))]
    [:scroll {:vertical-increment 16
              :border :none
              :listen [:mouse-wheel ::change-font-size]
              :margin-control [:line-number {:source editor :update-font true}]}
      [:panel {:border :none
               :layout :border}
        editor]]))

;; Document tab creation

(defn- document-tab
  "Creates a tab with an editor."
  [app doc]
  (let [id    (ui/genid)
        title (doc/name @doc)
        tool-tip (doc/path @doc)]
    (-> (tplts/tab id)
      (ui/update :tab ui/attr :tool-tip tool-tip)
      (ui/update :label ui/attr :text title)
      (ui/update :button ui/attr :stuff {:tab-id id})
      (ui/update :button ui/listen :click ::close-document-button)
      (ui/add (text-editor-create app doc))
      (ui/apply-stylesheet (:styles @app)))))

(defn- exit! [app e]
  (let [result (tplts/confirm "Bye bye" "Are you sure you want to leave this magical experience?")]
    (if (= result :ok)
        (System/exit 0))))

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
;;; Toogle Line Numbers

(defn- toggle-line-numbers
  "Hides and shows the line number for the currently active editor."
  [app e]
  (let [ui     (:ui @app)
        tab    (current-document-tab @ui)
        editor (ui/find tab :text-editor)]
    (when (and tab editor)
      (let [scroll       (ui/find tab :scroll)
            id           (ui/attr scroll :id)
            line-number  (ui/attr scroll :margin-control)]
        (if line-number
          (ui/update! ui (ui/selector# id) ui/attr :margin-control nil)
          (as-> [:line-number {:source editor}] line-number
            (ui/init line-number)
            (ui/apply-stylesheet line-number (:styles @app))
            (ui/update! ui (ui/selector# id) ui/attr :margin-control line-number)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Undo/Redo

(defn undo-redo! [app e f]
  (let [ui     (:ui @app)
        editor (current-text-editor @ui)]
    (when editor
      (let [id     (ui/attr editor :id)
            doc    (ui/attr editor :doc)
            hist   (doc/history @doc)]
        (swap! doc f)
        ;; TODO: Fix this abominable scheme for undo/redo
        (swap! doc assoc :read-only true)
        (let [[editor hist] (f editor hist)]
          (ui/update! ui (ui/selector# id) (constantly editor)))
        (swap! doc dissoc :read-only)))))

(defn redo! [app e]
  (undo-redo! app e doc/redo))

(defn undo! [app e]
  (undo-redo! app e doc/undo))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Next/previous center tab

(defn move-tab [app move]
  (let [ui    (:ui @app)
        tab   (current-document-tab @ui)
        tabs  (ui/find @ui :#center)
        children (ui/children tabs)
        i     (->> children
                (keep-indexed #(when (= tab %2) %1))
                first
                (move (count children)))]
    (ui/update! ui :#center ui/selection i)))

(defn next-tab [app e]
  (move-tab app
            (fn [total i] (if (< (inc i) total) (inc i) 0))))

(defn prev-tab [app e]
  (move-tab app
            (fn [total i] (if (>= (dec i) 0) (dec i) (dec total)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; 

(defn- goto-line-ok [app e]
  (let [dialog (ui/attr (:source e) :stuff)
        txt    (-> @dialog (ui/find :text-field) model/text)]
    (when (re-matches #"\d*" txt)
      (ui/update! dialog [] ui/attr :result :ok)
      (ui/update! dialog [] ui/attr :visible false))))

(defn- goto-line-cancel [app e]
  (let [dialog (ui/attr (:source e) :stuff)]
    (ui/update! dialog [] ui/attr :result :cancel)
    (ui/update! dialog [] ui/attr :visible false)))

(defn- goto-line! [app e]
  (let [ui     (:ui @app)
        editor (current-text-editor @ui)
        dialog (atom nil)]
    (when editor
      (reset! dialog (-> (tplts/line-number-dialog) 
                       ui/init
                       (ui/update :button ui/attr :stuff dialog)
                       (ui/update :#ok ui/listen :click ::goto-line-ok)
                       (ui/update :#cancel ui/listen :click ::goto-line-cancel)))
      (ui/attr @dialog :visible true)
      (when (= :ok (ui/attr @dialog :result))
        (ui/goto-line editor (-> @dialog (ui/find :text-field) model/text read-string))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Event handler

(defn keyword->fn [k]
  (or (-> k str (subs 1) symbol resolve)
      (throw (Exception. (str "The keyword " k " does not resolve to a var.")))))

(defn event-handler
  "Replaces the UI's default event-handler implementation, 
inserting a fixed first parameter, which is the app."
  [app f e]
  (cond
    (or (fn? f) (var? f))
      (f app e)
    (keyword? f)
      ((keyword->fn f) app e)
    (util/channel? f)
      (async/put! f [app e])
    :else
      (throw (Exception. "Not supported event handler value, it must be a function or a ns qualified keyword."))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Default styles

(def styles
  {#{:label :tree :button}
                {:font [:name "Consolas" :size 14]}
   #{:text-editor :text-area :scroll :split :panel :tree}
                {:border :none}
   :line-number {:font        [:name "Consolas" :size 16]
                 :background  0x666666
                 :color       0xFFFFFF
                 :current-line-color 0x00FFFF}
   #{:text-editor :text-area}
                {:font        [:name "Consolas" :size 16]
                 :background  0x333333
                 :color       0xFFFFFF
                 :caret-color 0xFFFFFF}
   :split       {:divider-size 3
                 :background 0x666666
                 :divider-background 0x999999}})

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
              {:category "View", :name "Show/Hide Line Numbers", :fn ::toggle-line-numbers, :keystroke "ctrl l"}
              {:category "View", :name "Next tab", :fn ::next-tab, :keystroke "ctrl tab"}
              {:category "View", :name "Prev tab", :fn ::prev-tab, :keystroke "ctrl alt tab"}

              {:category "Edit", :name "Goto line..." :fn ::goto-line! :keystroke "ctrl g"}
              {:category "Edit", :name "Undo", :fn ::undo!, :keystroke "ctrl z"}
              {:category "Edit", :name "Redo", :fn ::redo!, :keystroke "ctrl y"})])

(defn- init!
  "Builds the basic UI and adds it to the app under the key :ui."
  [app]
  (ui/register-event-handler! (partial #'event-handler app))
  (swap! app assoc :ui (-> @app
                         (lab/config :name)
                         tplts/app-window
                         (ui/update :#main ui/listen :closing ::exit!)
                         (ui/update :#center ui/listen :change ::switch-document-ui!)
                         (ui/apply-stylesheet styles)
                         atom)
                   :styles styles))

(plugin/defplugin lab.plugin.main-ui
  "Creates the UI for the application and hooks into
basic file operations."
  :init!    #'init!
  :hooks    hooks
  :keymaps  keymaps)
