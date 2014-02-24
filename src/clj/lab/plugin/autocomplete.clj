(ns lab.plugin.autocomplete
  (:require [clojure.zip :as zip]
            [lab.util :refer [timeout-channel]]
            [lab.core [plugin :as plugin]
                      [keymap :as km]
                      [lang :as lang]]
            [lab.ui.core :as ui]))

(defn popup-menu [app e]
  (let [ui     (:ui @app)
        editor (:source e)
        id     (ui/attr editor :id)
        pos    (ui/caret-position editor)
        location (ui/caret-location editor)
        popup  (-> [:pop-up-menu {:location location
                                  :source editor
                                  :border :none}
                     [:scroll {:size [150 100] :border :none}
                       [:tree]]]
                ui/init)]
    (ui/update! ui (ui/selector# id) ui/attr :popup-menu popup)
    (ui/attr popup :visible true)))

(defn- autocomplete [app {:keys [event description modifiers] :as e}]
  (when (and (= :pressed event) (= description :space) (modifiers :ctrl))
  (let [editor (:source e)
        pos    (ui/caret-position editor)
        doc    (ui/attr editor :doc)
        tree   (lang/code-zip (lang/parse-tree @doc))
        [loc i](lang/location tree pos)
        loc    (-> loc zip/prev)
        tag    (lang/location-tag loc)]
    (popup-menu app e))))

(defn- text-editor-hook [f doc]
  (let [editor (f doc)]
    (-> editor
      (ui/listen :key #'autocomplete))))

(def ^:private hooks
  {#'lab.ui.templates/text-editor #'text-editor-hook})

(plugin/defplugin lab.plugin.autocomplete
  :hooks hooks)
