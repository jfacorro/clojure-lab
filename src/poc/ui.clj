(ns poc.ui
  "Trying to define a DSL to abstract the UI
  components with Clojure data structures."
  (:require [poc.ui.protocols :as ui]
            [poc.ui.swing]))

(def menu
  {:tag     :menu-bar
   :content [{:tag :menu
              :title "File"
              :content [{:tag :menu-item :title "New" }
                        {:tag :menu-item :title "Open"}]}]})
       
(def text {:tag  :text-editor
           :text "Bla"})
           
(def tabs {:tag     :tabs
           :content [text]})

(def main {:tag     :window
           :title   "UI DSL"
           :size    [500 500]
           :menu    menu
           :content [tabs]})

(defn init []
  (-> main ui/create-component))
