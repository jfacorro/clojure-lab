(ns lab.ui.templates
  (:require [lab.ui.core :as ui]))

(defn- close-tab-button
  [app id & _]
  (let [ui  (:ui @app)
        tab (ui/find @ui (ui/selector# id))]
    (ui/update! ui (ui/parent id) ui/remove tab)))

(defn tab
  ([app stylesheet]
    (ui/with-id id
      (tab app id stylesheet)))
  ([app id stylesheet]
    (-> [:tab {:header [:panel {:transparent false :background 0xCCCCCC}
                          [:label {:color 0x000000}]
                            [:button {:icon         "close-tab.png"
                                      :border       :none
                                      :transparent  true
                                      :on-click     (partial #'close-tab-button app id)}]]}]
      (ui/apply-stylesheet stylesheet))))

(defn confirm
  [title message]
  [:option-dialog {:title   title
                   :message message
                   :options :yes-no-cancel
                   :visible true}])
