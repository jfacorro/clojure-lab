(remove-ns 'lab.ui)
(ns lab.ui
  "Trying to define a DSL to abstract the UI
  components with Clojure data structures."
  (:require [lab.ui.core :as ui :reload true]
            [lab.ui.tree :as tree]
            [lab.ui.protocols :as uip]
            [lab.ui.swing :as swing :reload true]))

(def ui (atom nil))

(defn create-text-editor [file]
  (ui/text-editor :text (slurp file)
                  :font (ui/font :name "Terminal" :size 10)))

(defn close-tab [& _]
  #_(let [tab (ui/find-by-id @ui :tab)]))

(defn create-tab [item]
  (ui/tab ;:ui/id  (keyword (gensym))
          :header (ui/panel [(ui/label :text (str item))
                             (ui/button :preferred-size [10 10]
                                        :on-click close-tab)])
          :content [(create-text-editor item)]))

(defn open-file [item]
  (let [tabs (ui/find-by-tag @ui :tabs)]
    (uip/add tabs (create-tab item))))

(def menu
  (ui/menu-bar [(ui/menu {:text "File"}
                         [(ui/menu-item :text "New!")
                          (ui/menu-item :text "Open")])]))

(def main (ui/window :title   "Clojure Lab"
                     :size    [700 500]
                     :menu    menu
                     :visible true
                     :content [(ui/split :orientation :horizontal
                                         :content [(tree/tree-from-path ".." open-file) (ui/tabs)])]))

(defn init [app]
  (reset! ui (ui/init main)))
  
(init nil)
nil