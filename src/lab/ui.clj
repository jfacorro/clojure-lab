(ns lab.ui
  "DSL to abstract the UIcomponents with Clojure data structures."
  (:require [lab.ui [core :as ui :reload true]
                    [select :as ui.sel :reload true]
                    [tree :as tree]
                    [menu :as menu]
                    [protocols :as uip]
                    [swing :reload true]]))

(defn- new-text-editor [file]
  (ui/text-editor :text        (slurp file)
                  :border      :none
                  :background  0x666666
                  :foreground  0xFFFFFF
                  :caret-color 0xFFFFFF
                  :font        [:name "Consolas" :size 14]
                  :on-insert   #(do (println %) (println "insert"))
                  :on-delete   #(do (println %) (println "delete"))
                  :on-change   #(do % (println "change"))))

(defn close-tab [ui id & _]
  (let [tab  (ui/find @ui (str "#" id))]
    (swap! ui ui/update :#documents uip/remove tab)))

(defn- new-tab [ui item]
  (let [id    (ui/genid)
        path  (.getCanonicalPath ^java.io.File item)
        text  (new-text-editor item)
        close (partial #'close-tab ui id)]
    (ui/tab :-id  id
            :-tool-tip path
            :-header   (ui/panel :opaque false
                                 :content [(ui/label :text (str item))
                                           (ui/button :icon "close-tab.png"
                                                      :border :none
                                                      :on-click close)])
            :border  :none
            :content text)))

(defn open-file [ui evt]
  (let [^java.io.File file (-> evt uip/source uip/get-selected)]
    (when (-> file .isDirectory not)
      (swap! ui ui/update :tabs uip/add (new-tab ui file)))))

(defn build-main [{ui :ui name :name :as app}]
  (ui/window :-id     "main"
             :title   name
             :size    [700 500]
             :icons   ["icon-16.png" "icon-32.png" "icon-64.png"]
             :menu    (ui/menu-bar)
             :content (ui/split :orientation :horizontal
                                :border      :none
                                :content [(ui/tree :-id "file-tree" 
                                                   :on-dbl-click (partial #'open-file ui)
                                                   :root (tree/load-dir ".."))
                                          (ui/tabs :-id "documents" :border :none)])))

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

(do
  (-> {:name "Clojure Lab - UI dummy"}
    init
    :ui
    (swap! ui/init)
    uip/show)
  nil)