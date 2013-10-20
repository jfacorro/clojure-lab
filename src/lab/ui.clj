(ns lab.ui
  "DSL to abstract the UIcomponents with Clojure data structures."
  (:require [lab.ui [core :as ui]
                    [select :as ui.sel]
                    [tree :as tree]
                    [menu :as menu]
                    [stylesheet :as css]
                    [protocols :as p]
                    swing]
             [lab.core.keymap :as km]
             [lab.model.document :as doc]))

(defn insert-text [app doc evt]
  (swap! doc doc/insert (:offset evt) (:text evt)))

(defn- create-text-editor [app doc]
  [:text-editor {:text        (doc/text @doc)
                 :border      :none
                 :background  0x666666
                 :foreground  0xFFFFFF
                 :caret-color 0xFFFFFF
                 :on-insert   (partial #'insert-text app doc)
                 :font        [:name "Monospaced.plain" :size 14]}])

(defn close-tab [ui id & _]
  (let [tab  (ui/find @ui (str "#" id))]
    (ui/update! ui :#documents p/remove tab)))

(defn- document-tab [app doc]
  (let [ui    (:ui app)
        id    (ui/genid)
        path  (doc/path @doc)
        text  (create-text-editor app doc)
        close (partial #'close-tab ui id)]
    [:tab {:-id       id
           :-doc      doc
           :-tool-tip path
           :-header   [:panel {:transparent true}
                              [:label {:text (doc/name @doc)}]
                              [:button {:icon        "close-tab.png"
                                        :border      :none
                                        :transparent true
                                        :on-click    close}]]
           :border    :none}
           text]))

(defn- current-document-tab [ui]
  (-> @ui (ui/find :#documents) p/selected))

(defn- open-document-ui [app doc]
  (as-> (:ui app) ui
    (ui/update! ui :#documents p/add (document-tab app doc))))

(defn- open-document-tree [app evt]
  (let [^java.io.File file (-> evt p/source p/selected)]
    (when-not (.isDirectory file)
      (lab.app/open-document app (.getCanonicalPath file)))))

(defn- open-document-menu
  [app evt]
  (let [file-dialog   (ui/init [:file-dialog {:-type :open}])
        [result file] (p/show file-dialog)]
    (if file
      (lab.app/open-document app (.getCanonicalPath file))
      app)))

(defn- open-document-hook
  [f app path]
  (let [app (f app path)
        doc (lab.app/current-document app)]
    (open-document-ui app doc)
    app))

(defn- new-document-hook
  [f app & evt]
  (let [app  (f app)
        doc  (lab.app/current-document app)]
    (open-document-ui app doc)
    app))

(defn- close-document-hook
  "Finds the currently selected tab, removes it and closes the document
associated to it."
  [f app & evt]
  (let [ui    (:ui app)
        tab   (current-document-tab ui)
        doc   (ui/get-attr tab :-doc)
        app   (f app @doc)]
    (ui/update! ui :#documents p/remove tab)
    app))

(defn- assign-path
  "If the document doesn't have a path, get one from the user."
  [doc]
  (if (doc/path doc)
    doc
    (let [file-dialog   (ui/init [:file-dialog {:-type :save}])
          [result file] (p/show file-dialog)]
      (if file
        (doc/bind doc (.getCanonicalPath file) :new? true)
        doc))))

(defn- save-document-hook
  [f app & evt]
  (let [ui    (:ui app)
        tab   (current-document-tab ui)
        doc   (ui/get-attr tab :-doc)]
    (swap! doc assign-path)
    (when (doc/path @doc)
      (f app doc))))

(defn- file-tree [app]
  [:tab {:-title "Files" :border :none}
        [:tree {:-id          "file-tree" 
                :on-dbl-click (partial #'open-document-tree app)
                :root         (tree/load-dir "/home/jfacorro/dev/clojure-lab/src/lab/ui/swing")}]])

(defn build-main [app-name]
  [:window {:-id     "main"
            :title   app-name
            :size    [700 500]
            :maximized true
            :icons   ["icon-16.png" "icon-32.png" "icon-64.png"]
            :menu    [:menu-bar]}
            [:split {:orientation :vertical
                     :resize-weight 1
                     :border :none}
                    [:split {:resize-weight 0}
                            [:tabs {:-id "left-controls"}]
                            [:split {:resize-weight 1}
                                     [:tabs {:-id "documents"}]
                                     [:tabs {:-id "right-controls"}]]]
                    [:tabs {:-id "bottom-controls"}]]])

(defn- register-keymap-hook
  [f app keymap]
  (case (:type keymap)
    :global
      (let [ui    (:ui @app)
            cmds  (-> keymap :bindings vals)]
        (swap! ui (partial reduce (partial menu/add-option app)) cmds))
     :lang  nil
     :local nil)
  (f app keymap))

(def hooks
  {#'lab.core.keymap/register! #'register-keymap-hook
   #'lab.app/new-document      #'new-document-hook
   #'lab.app/open-document     #'open-document-hook
   #'lab.app/save-document     #'save-document-hook
   #'lab.app/close-document    #'close-document-hook})

(def keymaps
  [(km/keymap (ns-name *ns*)
              :global
              {:category "File" :name "New" :fn #'lab.app/new-document :keystroke "ctrl N"}
              {:category "File" :name "Open" :fn #'open-document-menu :keystroke "ctrl O"}
              {:category "File" :name "Close" :fn #'lab.app/close-document :keystroke "ctrl W"}
              {:category "File" :name "Save" :fn #'lab.app/save-document :keystroke "ctrl S"})])

;; Init
(defn init!
  "Expects an atom containing the app. Builds the basic UI and 
  adds it to the app under the key :ui."
  [app]
  (let [ui (atom (-> (:name @app) build-main ui/init))]
    (swap! app assoc :ui ui)
    
    (ui/update! ui :#left-controls p/add (file-tree @app))
    ; comment out when testing == pretty bad workflow
    (p/show @ui)))

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
  (p/show @ui)
  nil)

)