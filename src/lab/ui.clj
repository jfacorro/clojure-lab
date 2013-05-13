(ns lab.ui
  "Trying to define a DSL to abstract the UI
  components with Clojure data structures."
  (:require [lab.ui.core :as ui :reload true]
            [lab.ui.tree :as tree]
            [lab.ui.protocols :as uip]
            [lab.ui.swing :as swing :reload true]))

(set! *warn-on-reflection* true)

(defn- new-text-editor [file]
  (ui/text-editor :text        (slurp file)
                  :border      :none
                  :background  0x333333
                  :foreground  0xFFFFFF
                  :caret-color 0xFFFFFF
                  :font        [:name "Consolas" :size 14]
                  :on-insert   #(do (println %) (println "insert"))
                  :on-delete   #(do (println %) (println "delete"))
                  :on-change   #(do % (println "change"))))

(defn close-tab [ui id & _]
  (let [tab  (ui/find @ui (str "#" id))]
    (swap! ui ui/update :tabs uip/remove tab)))

(defn- new-tab [ui item]
  (let [id   (ui/genid)
        path (.getCanonicalPath ^java.io.File item)
        text (new-text-editor item)
        text (ui/add-binding text "ctrl L" #(do % (println text)))
        text (ui/remove-binding text "ctrl L")]
    (ui/tab :-id  id
            :-tool-tip path
            :-header   (ui/panel :opaque false
                                 :content [(ui/label :text (str item))
                                           (ui/button :preferred-size [10 10]
                                                      :on-click (partial #'close-tab ui id))])
            :border  :none
            :content text)))

(defn open-file [ui evt]
  (let [^java.io.File file (-> evt uip/source uip/get-selected)]
    (when (-> file .isDirectory not)
      (swap! ui ui/update :tabs uip/add (new-tab ui file)))))

(def menu
  (ui/menu-bar [(ui/menu {:text "File"}
                         [(ui/menu-item :text "New")
                          (ui/menu-item :text "Open")])]))

(defn build-main [ui]
  (ui/window :title   "Clojure Lab - UI"
             :size    [700 500]
             :icons   ["icon-16.png" "icon-32.png" "icon-64.png"]
             :menu    menu
             :visible true
             :content (ui/split :orientation :horizontal
                                :border      :none
                                :content [(ui/tree :on-dbl-click (partial #'open-file ui) :root (tree/load-dir ".."))
                                          (ui/tabs :-id    :tabs
                                                   :border :none)])))

(defn build-menu [app]
  (let [bindings (:global-bindings app)]
    
    ))

(defn init [app]
  (let [ui   (atom nil)]
    (reset! ui (-> ui build-main ui/init))
    (assoc app :ui ui)))
  
(do (init nil) nil)
