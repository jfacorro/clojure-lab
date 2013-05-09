(ns lab.ui
  "Trying to define a DSL to abstract the UI
  components with Clojure data structures."
  (:require [lab.ui.core :as ui :reload true]
            [lab.ui.tree :as tree]
            [lab.ui.protocols :as uip]
            [lab.ui.swing :as swing :reload true]))

(set! *warn-on-reflection* true)

(def ^:dynamic *ui* (atom nil))

(defn- create-text-editor [file]
  (ui/text-editor :text        (slurp file)
                  :border      :none
                  :background  0x333333
                  :foreground  0xFFFFFF
                  :caret-color 0xFFFFFF
                  :font        [:name "Consolas" :size 14]
                  :on-insert   #(do % (println "insert"))
                  :on-delete   #(do % (println "delete"))
                  :on-change   #(do % (println "change"))))

(defn close-tab [id & _]
  (let [tab  (ui/find-by-id @*ui* id)
        tabs (ui/find-by-id @*ui* :tabs)]
    (uip/remove tabs tab)))

(defn- create-tab [item]
  (let [id   (keyword (gensym))
        path (.getCanonicalPath ^java.io.File item)]
    (ui/tab :-id  id
            :-tool-tip path
            :-header   (ui/panel :opaque false
                                 :content [(ui/label :text (str item))
                                           (ui/button :preferred-size [10 10]
                                                      :on-click (partial #'close-tab id))])
            :border  :none
            :content (create-text-editor item))))

(defn open-file [evt]
  (let [^java.io.File file (-> evt uip/source uip/get-selected)]
    (when (-> file .isDirectory not)
      (swap! *ui* ui/update-by-id :tabs #(uip/add % (create-tab file))))))

(def menu
  (ui/menu-bar [(ui/menu {:text "File"}
                         [(ui/menu-item :text "New!")
                          (ui/menu-item :text "Open")])]))

(def main (ui/window :title   "Clojure Lab - UI"
                     :size    [700 500]
                     :icons   ["icon-16.png" "icon-32.png" "icon-64.png"]
                     :menu    menu
                     :visible true
                     :content (ui/split :orientation :horizontal
                                        :border      :none
                                        :content [(ui/tree :on-dbl-click #'open-file :root (tree/load-dir ".." ))
                                                  (ui/tabs :-id    :tabs
                                                           :border :none)])))

(defn init [app]
  (reset! *ui* (ui/init main)))
  
;(init nil)
