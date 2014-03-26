(ns lab.plugin.connection
  (:refer-clojure :exclude [send])
  (:require [lab.core [plugin :refer [defplugin]]
                      [keymap :as km]]
            [lab.ui.core :as ui]))

(defprotocol Connection
  (send [this msg])
  (recv [this])
  (connect [this])
  (disconnect [this]))

(defn open-conn-view [owner]
  [:dialog {:id "open-connection"
            :title "Open connection"
            :owner owner
            :size [300 200]}
   [:panel {:layout [:box :page]}
    [:panel {:layout [:box :line]
             :padding 3}
     [:label {:text "Type "
              :width 40}]
     [:combobox {:background 0xFFFFFF}
      [:cb-item {:text "Clojure"}]
      [:cb-item {:text "Node JS"}]
      [:cb-item {:text "Python"}]]]
    [:panel {:layout [:box :line]
             :padding 3}
     [:label {:text "Url "
              :width 40}]
     [:text-field {:padding 0 :border [:line 0xAAAAAA 1]}]]
    [:panel {:height 500}]]])

(defn open-connection
  [{:keys [app source] :as e}]
  (let [ui      (:ui @app)
        dialog  (ui/init (open-conn-view @ui))]
    (ui/attr dialog :visible true)))

(def keymaps
  [(km/keymap 'lab.plugin.connection
    :global
    {:keystroke "ctrl alt c" :fn ::open-connection :category "Connection" :name "New"})])

(defplugin lab.plugin.connection
  :type    :global
  :keymaps keymaps)