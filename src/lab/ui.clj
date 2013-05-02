(remove-ns 'lab.ui)
(ns lab.ui
  "Trying to define a DSL to abstract the UI
  components with Clojure data structures."
  (:require [lab.ui.core :as ui :reload true]
            [lab.ui.swing :as swing :reload true]))

(def ui (atom nil))

(defn create-text-editor []
  (ui/text-editor :font (ui/font :name "Terminal" :size 10)))

(defn create-tab [item]
  (ui/tab :header (ui/panel [(ui/label :text item)
                             (ui/button :text "x" :size [10 10])])
          :content [(create-text-editor)]))

(defn add-tab [item]
  (let [tabs (-> @ui :content first :content second)]
    (lab.ui.protocols/add tabs (create-tab item))))

(def menu
  (ui/menu-bar [(ui/menu {:text "File"}
                         [(ui/menu-item :text "New!")
                          (ui/menu-item :text "Open")])]))

(def tree (ui/tree
            :on-dbl-click add-tab
            :root (ui/tree-node
                    :item "Project" 
                    :content [(ui/tree-node :item "macho.clj")
                              (ui/tree-node
                                :item "lab"
                                :content [(ui/tree-node :item "ui.clj")])])))

(def main (ui/window :title   "Clojure Lab"
                     :size    [500 300]
                     :menu    menu
                     :visible true
                     :content [(ui/split :orientation :horizontal
                                         :content [tree (ui/tabs)])]))

(defn init [app]
  (reset! ui (ui/init main)))
  
(init nil)
nil