(ns lab.plugin.clojure.macroexpand
  (:require [clojure.zip :as zip]
            [lab.core [plugin :refer [defplugin]]
                      [keymap :as km]
                      [lang :as lang]]
            [lab.model.protocols :as model]
            [lab.ui.core :as ui]
            [lab.plugin.clojure.nrepl :as nrepl]))

(defn- current-form [editor]
  (let [doc  (ui/attr editor :doc)
        root (lang/code-zip (lang/parse-tree @doc))
        pos  (ui/caret-position editor)
        [loc i] (lang/location root pos)
        delim-loc (lang/select-location loc zip/up #(-> % lang/location-tag (= :list)))]
    (when-let [[s e] (and delim-loc (lang/limits delim-loc))]
      (model/substring editor s e))))

(defn- show-popup
  [editor expansion]
  (let [location (ui/caret-location editor)
        popup (ui/init [:pop-up-menu {:size [500 200]
                                      :location location
                                      :source editor
                                      :border :none}
                        [:scroll {:border :none
                                  :vertical-increment 16}
                         [:panel {:border :none
                                  :layout :border}
                          [:text-editor {:border :none
                                         :text expansion
                                         :read-only true
                                         :line-highlight-color 0xCCCCCC
                                         :font ["Consolas" 14]}]]]])]
    (-> popup
      (ui/update :text-editor ui/caret-position 0)
      (ui/attr :visible true)
      (ui/update :text-editor ui/focus))))

(defn- macroexpansion
  [{:keys [app source] :as e}]
  (let [form `(macroexpand '~(read-string (current-form source)))
        expansion (nrepl/eval-and-get-value app (str form))
        result (with-out-str (clojure.pprint/pprint expansion))]
    (show-popup source result)))

(def ^:private keymaps
  [(km/keymap ::keymap
     :local
     {:keystroke "ctrl alt enter" :fn ::macroexpansion :name "In place macroexpansion"})])

(defplugin ::plugin
  :type :local
  :keymaps keymaps)

