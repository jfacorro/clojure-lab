(ns lab.ui.templates
  (:require [lab.ui.core :as ui]))

(defn- close-tab-button
  [app e]
  (let [ui  (:ui @app)
        id  (-> (:source e) (ui/attr :stuff) :tab-id)
        tab (ui/find @ui (ui/selector# id))]
    (ui/update! ui (ui/parent id) ui/remove tab)))

(defn tab
  "Creates a tab with a tab header as a panel that
includes a label and a closing button."
  ([app]
    (ui/with-id id (tab app id)))
  ([app id]
    (-> [:tab {:id id
               :header [:panel {:transparent false :background 0x333333}
                          [:label {:color 0xFFFFFF}]
                            [:button {:icon         "close-tab.png"
                                      :border       :none
                                      :transparent  true
                                      :stuff        {:tab-id id}
                                      :on-click     ::close-tab-button}]]}]
      (ui/apply-stylesheet (:styles @app)))))

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