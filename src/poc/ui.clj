(ns poc.ui
  "Trying to define a DSL to abstract the UI
  components with Clojure data structures."
  (:require [poc.ui.core :as ui]
            [poc.ui.swing :as swing]))

(def menu
  (ui/menu-bar [(ui/menu {:text "File"}
                         [(ui/menu-item :text "New!")
                          (ui/menu-item :text "Open")])]))

(def text (ui/text-editor))

(def tab (ui/tab :content [text]))

(def tabs (ui/tabs [tab]))

(def main (ui/window {:title   "Clojure Lab"
                      :size    [500 300]
                      :menu    menu
                      :visible true}
                     [tabs]))

(defn init []
  (ui/init main))

(init)