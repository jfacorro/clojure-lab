(ns lab.ui.templates
  (:require [lab.ui.core :as ui]
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

(defn- close-tab-button
  [app e]
  (let [ui  (:ui @app)
        id  (-> (:source e) (ui/attr :stuff) :tab-id)
        tab (ui/find @ui (ui/selector# id))]
    (ui/update! ui (ui/parent id) ui/remove tab)))

(defn tab
  "Creates a tab with a tab header as a panel that
includes a label and a closing button."
  ([]
    (tab (ui/genid)))
  ([id]
    (ui/init
      [:tab {:id id
             :header [:panel {:transparent false :background 0x333333}
                       [:label {:color 0xFFFFFF}]
                       [:button {:icon         "close-tab.png"
                                 :border       :none
                                 :transparent  true
                                 :listen       [:click ::close-tab-button]
                                 :stuff        {:tab-id id}}]]}])))

(defn text-editor
  "Creates a text editor with a document attached to it."
  [doc]
  (-> [:text-editor {:doc  doc}]
    ui/init
    (ui/attr :text (doc/text @doc))
    (ui/caret-position 0)))

(defn confirm
  [title message]
  (-> [:option-dialog {:title title, :message message, :options :yes-no-cancel, :visible true}]
    ui/init
    (ui/attr :result)))

(defn save-file-dialog
  [dir]
  [:file-dialog {:type :save
                 :visible true
                 :current-dir dir}])

(defn open-file-dialog
  [dir]
  [:file-dialog {:type :open
                 :visible true
                 :current-dir dir}])

(defn directory-dialog
  [title dir]
  [:file-dialog {:type           :open,
                 :selection-type :dir-only,
                 :visible        true,
                 :title          title
                 :current-dir    dir}])
