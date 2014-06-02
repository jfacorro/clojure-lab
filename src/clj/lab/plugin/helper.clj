(ns lab.plugin.helper
  (:require [lab.core [plugin :refer [defplugin]]
                      [keymap :as km]]
            [lab.ui.core :as ui]))

(defn- info-view
  [owner]
  [:dialog {:owner owner
            :modal true
            :size [600 500]
            :resizable false
            :background [0 0 0 0]
            :undecorated true}
   [:scroll {:border [:line [0 0 0 0x44] 2]
             :transparent true
             :vertical-increment 16}
    [:panel {:layout [:box :page]
             :padding 5
             :background [0 0 0]}]]])

(defn- section-label
  [txt]
  [:panel {:layout [:box :line]
           :transparent true}
   [:label {:text txt
            :color 0xFFFFFF
            :font [:size 16 :style :bold]}]
   [:panel {:transparent true}]])

(defn- command-label
  [{:keys [name category keystroke]}]
  [:panel {:layout [:box :line]
           :transparent true}
   [:label {:text (str (or (and category (str category " - "))) 
                    name)
            :color 0xFFFFFF
            :font [:size 14]}]
   [:panel {:transparent true
            :border [:line 0x333333 [0 0 1 0]]
            :padding [0 10]}]
   [:label {:text (.toUpperCase ^String (str keystroke))
            ;; :width 100
            :color 0xFFFFFF
            :font [:size 14 :style :bold]}]])

(defn- default-local-keymap
  [x]
  (->> (ui/listeners x :key)
    (filter map?)
    first))

(defmulti local-keymap
  "Takes a UI component and returns its local keymap."
  :tag)

(defmethod local-keymap :default
  [x]
  (default-local-keymap x))

(defmethod local-keymap :text-editor
  [x]
  (if-let [doc (ui/attr x :doc)]
    (:keymap @doc)
    (default-local-keymap x)))

(defn- commands-list
  [app source]
  (let [global (:keymap @app)
        local  (local-keymap source)]
    (reduce
      (fn [labels [k cmds]]
        (reduce
          (fn [labels cmd]
            (if (:keystroke cmd)
              (conj labels (command-label cmd))
              labels))
          (conj labels (section-label (str k)))
          (sort-by :category cmds)))
      []
      (mapcat km/commands [local global]))))

(defn- help-info
  [{:keys [app source] :as e}]
  (let [ui (:ui @app)]
    (-> (info-view @ui)
      ui/init
      (ui/update :panel ui/add-all (commands-list app source))
      (ui/attr :visible true))))

(def ^:private keymaps
  [(km/keymap "Contextual Help"
     :global
     {:category "Help" :name "Show Info" :fn ::help-info :keystroke "f1"})])

(defplugin (ns-name *ns*)
  :type :global
  :keymaps keymaps)
