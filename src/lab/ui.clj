(ns lab.ui
  "DSL to abstract the UIcomponents with Clojure data structures."
  (:require [lab.ui [core :as ui :reload true]
                    [select :as ui.sel :reload true]
                    [tree :as tree]
                    [menu :as menu]
                    [protocols :as uip]
                    [swing :reload true]]))

(defn- new-text-editor [file]
  [:text-editor {:text        (slurp file)
                 :border      :none
                 :background  0x666666
                 :foreground  0xFFFFFF
                 :caret-color 0xFFFFFF
                 :font        [:name "Consolas" :size 14]
                 :on-insert   #(do (println %) (println "insert"))
                 :on-delete   #(do (println %) (println "delete"))
                 :on-change   #(do % (println "change"))}])

(defn close-tab [ui id & _]
  (let [tab  (ui/find @ui (str "#" id))]
    (swap! ui ui/update :#documents uip/remove tab)))

(defn- new-tab [ui item]
  (let [id    (ui/genid)
        path  (.getCanonicalPath ^java.io.File item)
        text  (new-text-editor item)
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

(defn open-file [ui evt]
  (let [^java.io.File file (-> evt uip/source uip/get-selected)]
    (when-not (.isDirectory file)
      (swap! ui ui/update :#documents uip/add (new-tab ui file)))))

(defn build-main [{ui :ui name :name :as app}]
  [:window {:-id     "main"
            :title   name
            :size    [700 500]
            :icons   ["icon-16.png" "icon-32.png" "icon-64.png"]
            :menu    [:menu-bar]}
            [:split {:orientation      :vertical
                     :border           :none
                     :divider-location 100}
                    [:split {:one-touch-expandable true
                             :divider-location 0.2
                             :resize-weight 0
                             :border :none}
                            [:tabs {:-id "left-controls" :border :none}
                                   [:tab {:-title "Files" :border :none}
                                         [:tree {:-id          "file-tree" 
                                                 :on-dbl-click (partial #'open-file ui)
                                                 :root         (tree/load-dir "/home/jfacorro/Downloads/clojure-lab/src/lab/ui/swing")}]]]
                            [:split {:one-touch-expandable true
                                     :divider-location 0.8
                                     :resize-weight 1
                                     :border :none}
                                     [:tabs {:border :none :-id "documents"}]
                                     [:tabs {:-id "right-controls"}]]]
                    [:tabs {:-id "bottom-controls"}]]])

;; Init
(defn init [app]
  (let [ui  (atom nil)
        app (assoc app :ui ui)]
    (reset! ui (-> app build-main ui/init))
    (do
      (swap! ui menu/add-option {:menu "File" :name "New" :action #(println "New" (class %2)) :key-stroke "ctrl N"})
      (swap! ui menu/add-option {:menu "File" :name "Open" :action #(println "Open" (class %2)) :key-stroke "ctrl O"})
      (swap! ui menu/add-option {:menu "File -> Project" :name "New" :action #(println "New Project" (class %2))})
      (swap! ui menu/add-option {:menu "File" :separator true})
      (swap! ui menu/add-option {:menu "File" :name "Exit" :action #(do %& (System/exit 0))})
      (swap! ui menu/add-option {:menu "Edit" :name "Copy" :action #(println "Exit" (class %2))}))
    app))

#_(do
  (def x
    (let [app (init {:name "Clojure Lab - UI dummy"})
          ui  (app :ui)]
      (swap! ui ui/init)
      ui))
  (uip/show @x)
  
  (require '[lab.ui.stylesheet :as css :reload true])
  (def stylesheet {:#documents {:border [:line 0x0000FF 5]}})
  (css/apply-stylesheet x stylesheet)
  nil)
