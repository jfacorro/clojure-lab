(ns lab.ui
  "DSL to abstract the UIcomponents with Clojure data structures."
  (:require [lab.ui [core :as ui]
                    [select :as ui.sel]
                    [tree :as tree]
                    [menu :as menu]
                    [stylesheet :as css]
                    [protocols :as p]
                    swing]))

(defn- create-text-editor [file]
  [:text-editor {:text        (slurp file)
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

(defn- close-document
  "Closes the current document."
  [app evt]
  (let [ui    (:ui @app)
        docs  (ui/find @ui :#documents)
        tab   (nth (p/children docs) (p/get-selected docs))]
    (ui/update! ui :#documents p/remove tab))
  app)

(defn- document-tab [ui item]
  (let [id    (ui/genid)
        path  (.getCanonicalPath ^java.io.File item)
        text  (create-text-editor item)
        close (partial #'close-tab ui id)]
    [:tab {:-id  id
           :-tool-tip path
           :-header   [:panel {:opaque false}
                              [:label {:text (str item)}]
                              [:button {:icon     "close-tab.png"
                                        :border   :none
                                        :on-click close}]]
           :border  :none}
           text]))

(defn- open-document [app file]
  (when-not (.isDirectory file)
    (as-> (:ui @app) ui
      (ui/update! ui :#documents p/add (document-tab ui file)))))

(defn- on-file-selection [app evt]
  (let [^java.io.File file (-> evt p/source p/get-selected)]
    (open-document app file)))

(defn- open-file [app evt]
  (let [file-dialog   (ui/init [:file-dialog {:-type :open}])
        [result file] (p/show file-dialog)]
    (when file
      (open-document app file)
      #_(f app file))))

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
                     :divider-location 200}
                    [:split {:divider-location 200
                             :resize-weight 0}
                            [:tabs {:-id "left-controls"}]
                            [:split {:divider-location 200
                                     :resize-weight 1}
                                     [:tabs {:-id "documents"}]
                                     [:tabs {:-id "right-controls"}]]]
                    [:tabs {:-id "bottom-controls"}]]])

;; Init
(defn init!
  "Expects an atom containing the app. Builds the basic UI and 
  adds it to the app under the key :ui."
  [app]
  (let [ui (atom (-> (:name @app) build-main ui/init))]
    (swap! app assoc :ui ui)
    
    (ui/update! ui :#left-controls p/add (file-tree app))
    (do
      (swap! ui menu/add-option app {:category "File" :name "New" :fn #(println "New" (class %2)) :keystroke "ctrl N"})
      (swap! ui menu/add-option app {:category "File" :name "Open" :fn #'open-file :keystroke "ctrl O"})
      (swap! ui menu/add-option app {:category "File" :name "Close" :fn #'close-document :keystroke "ctrl W"})
      (swap! ui menu/add-option app {:category "File > History" :name "Show" :fn #(println "History Show" (class %2)) :keystroke "ctrl B"})
      (swap! ui menu/add-option app {:category "File" :separator true})
      (swap! ui menu/add-option app {:category "File" :name "Exit" :fn #(do %& (System/exit 0))})
      (swap! ui menu/add-option app {:category "Edit" :name "Copy" :fn #(println "Exit" (class %2))}))
    ; comment out when testing == pretty bad workflow
    (p/show @ui)))

(comment

(do
  (def x
    (let [app (init {:name "Clojure Lab - UI dummy"})
          ui  (app :ui)]
      ui))
  (def stylesheet {:split {:border :none}
                   :tabs  {:border :none}
                   :text-editor {:background 0x555555
                                 :font       [:name "Monospaced.plain" :size 14]}})
  (css/apply-stylesheet x stylesheet)
  (p/show @x)
  nil)

)