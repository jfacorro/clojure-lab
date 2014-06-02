(ns lab.plugin.clojure.macroexpand
  (:require [clojure.zip :as zip]
            [lab.core [plugin :refer [defplugin]]
                      [keymap :as km]
                      [lang :as lang]]
            [lab.model.protocols :as model]
            [lab.ui.core :as ui]
            [lab.plugin.clojure.nrepl :as nrepl]))

(defn- current-form
  "Takes a text editor and returns a string with the contents
  of the inner-most form found in the current position."
  [editor]
  (let [doc  (ui/attr editor :doc)
        root (lang/code-zip (lang/parse-tree @doc))
        pos  (ui/caret-position editor)
        [loc i] (lang/location root pos)
        delim-loc (lang/select-location loc zip/up #(-> % lang/location-tag (= :list)))]
    (when-let [[s e] (and delim-loc (lang/limits delim-loc))]
      (model/substring editor s e))))

(defn- show-popup
  [app editor expansion]
  (let [location (ui/caret-location editor)
        popup (ui/init [:pop-up-menu {:size [500 200]
                                      :location location
                                      :source editor
                                      :border :none}
                        [:scroll
                         [:panel {:layout :border}
                          [:text-editor {:text expansion
                                         :read-only true
                                         :line-highlight-color [0 0 0 0]}]]]])]
    (-> popup
      (ui/update :text-editor ui/caret-position 0)
      (ui/attr :visible true)
      (ui/update :text-editor ui/focus)
      (ui/apply-stylesheet (:styles @app)))))

(defn- macroexpansion
  "Shows the current form's macroexpansion in a popup."
  [{:keys [app source] :as e}]
  (when-let [form (current-form source)]
    (let [code (when form `(macroexpand '~(read-string form)))
          expansion (nrepl/eval-and-get-value app (str code))
          result (with-out-str (clojure.pprint/pprint expansion))]
      (show-popup app source result))))

(def ^:private keymaps
  [(km/keymap "Macroexpand"
     :local
     {:keystroke "ctrl alt enter" :fn ::macroexpansion :name "Inline macroexpansion"})])

(defplugin ::plugin
  :type :local
  :keymaps keymaps)

