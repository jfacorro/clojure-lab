(ns lab.ui
  "Trying to define a DSL to abstract the UI
  components with Clojure data structures."
  (:require [lab.ui.core :as ui :reload true]
            [lab.ui.swing :as swing :reload true]))

(def menu
  (ui/menu-bar [(ui/menu {:text "File"}
                         [(ui/menu-item :text "New!")
                          (ui/menu-item :text "Open")])]))

(def text (ui/text-editor))

(def tab (ui/tab :content [text]))

(def tabs (ui/tabs [tab]))

(def tree (ui/tree
            :on-dbl-click #(println "dbl-click" %)
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
                                         :content [tree tabs])]))

(defn init [app]
  (ui/init main))
  
(init nil)