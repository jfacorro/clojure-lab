(ns lab.ui
  "Trying to define a DSL to abstract the UI
  components with Clojure data structures."
  (:require [lab.ui.core :as ui :reload true]
            [lab.ui.tree :as tree]
            [lab.ui.protocols :as uip]
            [lab.ui.swing :as swing :reload true]))

(set! *warn-on-reflection* true)

(def ui (atom nil))

(defn- create-text-editor [file]
  (ui/text-editor :text (slurp file)
                  :font (ui/font :name "Consolas" :size 14)))

(defn close-tab [id & _]
  (let [tab  (ui/find-by-id @ui id)
        tabs (ui/find-by-id @ui :tabs)]
    (uip/remove tabs tab)))

(defn- create-tab [item]
  (let [id (keyword (gensym))]
    (ui/tab :ui/id  id
            :border nil
            :header (ui/panel :opaque false 
                              :content [(ui/label :text (str item))
                                        (ui/button :preferred-size [10 10]
                                          :on-click (partial #'close-tab id))])
            :content [(create-text-editor item)])))

(defn open-file [item]
  (let [tabs (ui/find-by-id @ui :tabs)]
    (swap! ui ui/update-by-id :tabs #(uip/add % (create-tab item)))))

(def menu
  (ui/menu-bar [(ui/menu {:text "File"}
                         [(ui/menu-item :text "New!")
                          (ui/menu-item :text "Open")])]))

(def main (ui/window :title   "Clojure Lab"
                     :size    [700 500]
                     :menu    menu
                     :visible true
                     :content [(ui/split :orientation :horizontal
                                         :border      :bla
                                         :content [(tree/tree-from-path ".." open-file) (ui/tabs :ui/id :tabs :border :bla)])]))

(defn init [app]
  (reset! ui (ui/init main)))
  
(init nil)
nil