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

(defn- create-text-editor [txt]
  [:text-editor {:text        txt
                 :border      :none
                 :background  0x666666
                 :foreground  0xFFFFFF
                 :caret-color 0xFFFFFF
                 :font        [:name "Monospaced.plain" :size 14]
                 :on-insert   #(do (println %) (println "insert"))
                 :on-delete   #(do (println %) (println "delete"))
                 :on-change   #(do % (println "change"))}])

(defn close-tab [ui id & _]
  (let [tab  (ui/find @ui (str "#" id))]
    (ui/update! ui :#documents p/remove tab)))

(defn- close-document-hook
  "Closes the current document."
  [f app & evt]
  (let [ui    (:ui app)
        docs  (ui/find @ui :#documents)
        tab   (nth (p/children docs) (p/get-selected docs))
        doc   (ui/get-attr tab :-doc)
        app   (f app doc)]
    (println doc)
    (ui/update! ui :#documents p/remove tab)
    app))

(defn- document-tab [app doc]
  (let [ui    (:ui app)
        id    (ui/genid)
        path  (doc/path @doc)
        text  (create-text-editor (doc/text @doc))
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

(defn- open-document [app doc]
  (as-> (:ui app) ui
    (ui/update! ui :#documents p/add (document-tab app doc))))

(defn- on-file-selection [app evt]
  (let [^java.io.File file (-> evt p/source p/get-selected)]
    (when-not (.isDirectory file)
      (lab.app/open-document app (.getCanoncialPath file)))))

(defn- open-document-hook
  [f app & evt]
  (let [file-dialog   (ui/init [:file-dialog {:-type :open}])
        [result file] (p/show file-dialog)]
    (if-not file
      app
      (let [app (f app (.getCanonicalPath file))
            doc (lab.app/current-document app)]
        (println doc)
        (open-document app doc)
        app))))

(defn- new-document-hook
  [f app & evt]
  (let [app  (f app)
        doc  (lab.app/current-document app)]
    (open-document app doc)
    app))

(defn- file-tree [app]
  [:tab {:-title "Files" :border :none}
        [:tree {:-id          "file-tree" 
                :on-dbl-click (partial #'on-file-selection app)
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
                     :divider-location 200
                     :border :none}
                    [:split {:divider-location 200
                             :resize-weight 0}
                            [:tabs {:-id "left-controls"}]
                            [:split {:divider-location 200
                                     :resize-weight 1}
                                     [:tabs {:-id "documents"}]
                                     [:tabs {:-id "right-controls"}]]]
                    [:tabs {:-id "bottom-controls"}]]])

(defn- register-keymap-hook
  [f app keymap]
  (case (:type keymap)
    :global
      (let [ui    (:ui app)
            cmds  (-> keymap :bindings vals)]
        (swap! ui (partial reduce (partial menu/add-option app)) cmds))
     :lang  nil
     :local nil)
  (f app keymap))

(def hooks
  {#'lab.core.keymap/register  #'register-keymap-hook
   #'lab.app/new-document      #'new-document-hook
   #'lab.app/open-document     #'open-document-hook
   #'lab.app/close-document    #'close-document-hook})

(def keymaps
  [(km/keymap (ns-name *ns*)
              :global
              {:category "File" :name "New" :fn #'lab.app/new-document :keystroke "ctrl N"}
              {:category "File" :name "Open" :fn #'lab.app/open-document :keystroke "ctrl O"}
              {:category "File" :name "Close" :fn #'lab.app/close-document :keystroke "ctrl W"})])

;; Init
(defn init!
  "Expects an atom containing the app. Builds the basic UI and 
  adds it to the app under the key :ui."
  [app]
  (let [ui (atom (-> (:name @app) build-main ui/init))]
    (swap! app assoc :ui ui)
    
    (ui/update! ui :#left-controls p/add (file-tree app))
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

(do
  (swap! ui reduce (partial menu/add-option app)
                   [{:category "File" :name "New" :fn #'new-file :keystroke "ctrl N"}
                    {:category "File" :name "Open" :fn #'open-file :keystroke "ctrl O"}
                    {:category "File" :name "Close" :fn #'close-document :keystroke "ctrl W"}
                    {:category "File > History" :name "Show" :fn #(println "History Show" (class %2)) :keystroke "ctrl B"}
                    {:category "File" :separator true}
                    {:category "File" :name "Exit" :fn #(do %& (System/exit 0))}
                    {:category "Edit" :name "Copy" :fn #(println "Exit" (class %2))}]))

)