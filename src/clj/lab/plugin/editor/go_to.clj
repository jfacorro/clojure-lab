(ns lab.plugin.editor.go-to
  (:require [lab.core [keymap :as km]
                      [plugin :refer [defplugin]]
                      [main :refer [current-text-editor]]]
            [lab.model.protocols :as model]
            [lab.ui.core :as ui]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; View

(defn view [owner dialog]
  (let [ok-btn (ui/init [:button {:id "ok"
                                  :text "Ok"
                                  :stuff {:dialog dialog}
                                  :listen [:click ::goto-line-ok]}])]
    [:dialog {:owner owner
              :icons ["right-icon.png"]
              :title "Go to Line"
              :size  [300 85]
              :modal true
              :default-button ok-btn}
      [:panel {:layout [:box :page]}
        [:text-field {:border :none}]
        [:panel {:layout :flow}
          ok-btn
          [:button {:id "cancel"
                    :text "Cancel"
                    :listen [:click ::goto-line-cancel]}]]]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Go to line

(defn- goto-line-ok
  [{:keys [source] :as e}]
  (let [dialog (:dialog (ui/stuff source))
        txt    (model/text (ui/find @dialog :text-field))]
    (when (re-matches #"\d*" txt)
      (ui/update! dialog [] ui/attr :result :ok)
      (ui/update! dialog [] ui/attr :visible false))))

(defn- goto-line-cancel
  [{:keys [source] :as e}]
  (let [dialog (:dialog (ui/stuff source))]
    (ui/update! dialog [] ui/attr :result :cancel)
    (ui/update! dialog [] ui/attr :visible false)))

(defn- goto-line!
  [e]
  (let [app    (:app e)
        ui     (:ui @app)
        editor (current-text-editor @ui)
        dialog (atom nil)]
    (when editor
      (-> dialog
          (reset! (ui/init (view @ui dialog)))
          (ui/attr :visible true))
      (when (= :ok (ui/attr @dialog :result))
        (ui/goto-line editor (-> @dialog (ui/find :text-field) model/text read-string))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Plugin Definition

(def ^:private keymaps
  [(km/keymap "Go to line"
     :global
     {:category "Edit", :name "Go to Line" :fn ::goto-line! :keystroke "ctrl g"})])

(defplugin lab.plugin.editor.go-to
  "Implements the functionality 'go to line of current documet'."
  :type     :global
  :keymaps  keymaps)
