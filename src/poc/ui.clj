(remove-ns 'poc.ui)
(ns poc.ui
  "Trying to define a DSL to abstract the UI
  components with Clojure data structures."
  (:require [poc.ui.protocols :as ui :reload true]
            [poc.ui.swing :as swing :reload true]))

(def menu
  (ui/menu-bar [(ui/menu {:text "File"} 
                         [(ui/menu-item :text "New!") 
                          (ui/menu-item :text "Open")])]))

(def text (ui/text-editor :text "Some text in the text editor. Oh yeah!"))

(def tab (ui/tab ;:layout (swing/border-layout)
                 :content [text]))

(def tabs (ui/tabs [(ui/scroll [text])]))

(def main (ui/window {:title   "Clojure Lab" 
                      :size    [500 300]
                      :menu    menu
                      :visible true}
                     [tabs]))

(defn init []
  (ui/init main))
