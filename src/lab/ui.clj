(ns lab.ui
  "DSL to abstract the UIcomponents with Clojure data structures."
  (:require [lab.ui [core :as ui]
                    [select :as ui.sel]
                    [tree :as tree]
                    [menu :as menu]
                    [stylesheet :as css]
                    [protocols :as p]
                    [swing]]))

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
  [{ui :ui :as app} evt]
  (let [docs  (ui/find @ui :#documents)
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

(defn- open-document [{ui :ui :as app} file]
  (when-not (.isDirectory file)
    (ui/update! ui :#documents p/add (document-tab ui file))))

(defn- on-file-selection [app evt]
  (let [^java.io.File file (-> evt p/source p/get-selected)]
    (open-document app file)))

(defn- open-file [app evt]
  (let [file-dialog   (ui/init [:file-dialog {:-type :open}])
        [result file] (p/show file-dialog)]
    (when file
      (open-document app file)
      #_(f app file))))

(defn build-main [{ui :ui name :name :as app}]
  [:window {:-id     "main"
            :title   name
            :size    [700 500]
            :icons   ["icon-16.png" "icon-32.png" "icon-64.png"]
            :menu    [:menu-bar]}
            [:split {:one-touch-expandable true
                     :orientation :vertical
                     :resize-weight 1}
                    [:split {:one-touch-expandable true
                             :divider-location 100
                             :resize-weight 0}
                            [:tabs {:-id "left-controls"}
                                   [:tab {:-title "Files" :border :none}
                                         [:tree {:-id          "file-tree" 
                                                 :on-dbl-click (partial #'on-file-selection app)
                                                 :root         (tree/load-dir "/home/jfacorro/Downloads/clojure-lab/src/lab/ui/swing")}]]]
                            [:split {:one-touch-expandable true
                                     :divider-location 0.8
                                     :resize-weight 1}
                                     [:tabs {:-id "documents"}]
                                     [:tabs {:-id "right-controls"}]]]
                    [:tabs {:-id "bottom-controls"}]]])

(def key-map {"ctrl O" {:menu "File" :name "Open" :action #'open-document :key-stroke "ctrl O"}})

;; Init
(defn init [app]
  (let [ui  (atom nil)
        app (assoc app :ui ui)
        km  (:key-map app)]
    (reset! ui (-> app build-main ui/init))
    
    (do
      (swap! ui menu/add-option app {:menu "File" :name "New" :action #(println "New" (class %2)) :key-stroke "ctrl N"})
      (swap! ui menu/add-option app {:menu "File" :name "Open" :action #'open-file :key-stroke "ctrl O"})
      (swap! ui menu/add-option app {:menu "File" :name "Close" :action #'close-document :key-stroke "ctrl W"})
      (swap! ui menu/add-option app {:menu "File -> History" :name "Show" :action #(println "History Show" (class %2)) :key-stroke "ctrl B"})
      (swap! ui menu/add-option app {:menu "File" :separator true})
      (swap! ui menu/add-option app {:menu "File" :name "Exit" :action #(do %& (System/exit 0))})
      (swap! ui menu/add-option app {:menu "Edit" :name "Copy" :action #(println "Exit" (class %2))}))
    ; (p/show @ui) ; comment out when testing == pretty bad workflow
    app))

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

;(-> [:file-dialog {:-type :save}] ui/init p/show)