(ns lab.plugin.helper
  (:require [lab.core [plugin :refer [defplugin]]
                      [keymap :as km]]
            [lab.ui.core :as ui]))

(defn- info-view
  [owner]
  [:dialog {:owner owner
            :modal true
            :size [500 500]
            :resizable false
            :undecorated true
            :opacity 1}
   [:scroll {:border :none}
    [:panel {:layout [:box :page]
             :padding 5
             :background [0 0 0 255]}]]])

(defn- section-label [txt]
  [:label {:text txt
           :color 0xFFFFFF
           :font [:size 16 :style :bold]}])

(defn- command-label
  [{:keys [name category keystroke]}]
  [:label {:text (str "[" (.toUpperCase keystroke) "] " category " - " name)
           :color 0xFFFFFF
           :font [:size 14]}])

(defn- commands-list [app source]
  (let [global (:keymap @app)]
    (reduce-kv
      (fn [labels k cmds]
        (reduce
          (fn [labels cmd]
            (if (:keystroke cmd)
              (conj labels (command-label cmd))
              labels))
          (conj labels (section-label (str k)))
          cmds))
      []
      (km/commands global))))

(defn- help-info
  [{:keys [app source] :as e}]
  (let [ui (:ui @app)]
    (-> (info-view @ui)
      ui/init
      (ui/update :panel ui/add-all (commands-list app source))
      (ui/attr :visible true))))

(def ^:private keymaps
  [(km/keymap (ns-name *ns*)
     :global
     {:category "Help" :name "Show Info" :fn ::help-info :keystroke "f1"})])

(defplugin (ns-name *ns*)
  :type :global
  :keymaps keymaps)
