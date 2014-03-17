(ns lab.ui.templates
  (:require [lab.util :refer [index-of]]
            [lab.ui.core :as ui]
            [lab.model.document :as doc]))

(defn app-window [app-name]
  (ui/init
    [:window {:id     "main"
              :title   app-name
              :visible true
              :size    [700 500]
              :maximized true
              :icons   ["icon-16.png" "icon-32.png" "icon-64.png"]
              :menu    [:menu-bar]}
      [:split {:orientation :vertical
               :resize-weight 1}
        [:split {:resize-weight 0
                 :divider-location 150}
          [:tabs {:id "left" :border :none}]
          [:split {:resize-weight 1}
            [:tabs {:id "center"}]
            [:tabs {:id "right"}]]]
        [:tabs {:id "bottom"}]]]))

(defn close-tab
  "Expects the source of the event to have a :tab-id key in its :stuff."
  [e]
  (let [ui  (-> e :app deref :ui)
        id  (-> (:source e) (ui/attr :stuff) :tab-id)
        tab (ui/find @ui (ui/id= id))]
    (ui/update! ui (ui/parent id) ui/remove tab)))

(defn- resolve-close-tab [e]
  (let [ui  (-> e :app deref :ui)
        id  (-> (:source e) (ui/attr :stuff) :tab-id)
        tab (ui/find @ui (ui/id= id))
        close-tab (or (:close-tab (ui/attr tab :stuff)) close-tab)]
    (when close-tab
      (close-tab e))))

(defn select-tab [tabs id]
  (ui/selection tabs
                (index-of (ui/children tabs)
                          (ui/find tabs (ui/id= id)))))

(defn- select-tab-click
  "Selects the tab that generated the event."
  [e]
  (let [ui  (-> e :app deref :ui)
        id  (-> (:source e) (ui/attr :stuff) :tab-id)]
    (ui/update! ui (ui/parent id) select-tab id)))

(defn- tab-click
  [e]
  (case (:button e)
    :button-1 (select-tab-click e)
    :button-2 (resolve-close-tab e)
    :button-3 nil))

(defn tab
  "Creates a tab with a tab header as a panel that
includes a label and a closing button."
  ([]
    (tab (ui/genid)))
  ([id]
    (ui/init
      [:tab {:id id
             :header [:panel {:transparent false
                              :background 0x333333
                              :stuff  {:tab-id id}
                              :listen [:click ::tab-click]}
                       [:label {:color 0xFFFFFF}]
                       [:button {:icon         "close-tab.png"
                                 :border       :none
                                 :transparent  true
                                 :listen       [:click ::resolve-close-tab]
                                 :stuff        {:tab-id id}}]]}])))

(defn text-editor
  "Creates a text editor with a document attached to it."
  [doc]
  (-> [:text-editor {:doc  doc}]
    ui/init
    (ui/attr :text (doc/text @doc))
    (ui/caret-position 0)))

(defn confirm
  [title message owner]
  (-> [:option-dialog {:owner owner, :title title, :message message, :options :yes-no-cancel, :visible true}]
    ui/init
    (ui/attr :result)))

(defn save-file-dialog
  [dir owner]
  [:file-dialog {:owner owner
                 :type :save
                 :visible true
                 :current-dir dir}])

(defn open-file-dialog
  [dir owner]
  [:file-dialog {:owner owner
                 :type :open
                 :visible true
                 :current-dir dir}])

(defn directory-dialog
  [title dir owner]
  [:file-dialog {:owner owner
                 :type           :open,
                 :selection-type :dir-only,
                 :visible        true,
                 :title          title
                 :current-dir    dir}])

(defn search-file-dialog
  "Defines the dialog that's used to search and
open files."
  [owner title]
  [:dialog {:id "search-file"
            :title title
            :size  [500 150]
            :modal true
            :owner owner}
    [:panel {:layout [:box :page]}
      [:text-field {:border :none}]
      [:panel {:layout :border}
        [:scroll {:border :none}
          [:tree {:id "results" :hide-root true}
            [:tree-node {:item :root}]]]]]])

(defn search-text-dialog [owner title]
  [:dialog {:id "search-text"
            :title title
            :size  [500 150]
            :modal false
            :owner owner}
    [:panel {:layout [:box :page]}
      [:panel {:layout [:box :line]}
        [:text-field {:border :none}]
        [:button {:text "Search"}]]
      [:panel {:layout :border}
        [:scroll [:tree {:hide-root true}
                   [:tree-node {:item :root}]]]]]])

(defn line-number-dialog [owner]
  [:dialog {:owner owner
            :title "Enter Line Number"
            :size  [300 85]
            :modal true}
    [:panel {:layout [:box :page]}
      [:text-field {:border :none}]
      [:panel {:layout :flow}
        [:button {:id "ok" :text "Ok"}]
        [:button {:id "cancel" :text "Cancel"}]]]])
