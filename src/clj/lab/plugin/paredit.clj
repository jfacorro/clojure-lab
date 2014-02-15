(ns lab.plugin.paredit
  (:require [lab.ui.core :as ui]
            [lab.util :refer [timeout-channel find-limits]]
            [lab.model.document :as doc]
            [lab.model.protocols :as model]
            [lab.core [plugin :as plugin]
                      [lang :as lang]
                      [keymap :as km]]))

(defn- insert-tab [app e]
  (let [editor (:source e)
        offset (ui/caret-position editor)]
    (model/insert editor offset "  ")))

(def delimiters
  {\( \)
   \[ \]
   \{ \}
   \" \"})

(def ignore? #{:net.cgrand.parsley/unfinished
               :net.cgrand.parsley/unexpected
               :string :comment :char :regex})

(defn- balance-delimiter [app e]
  (let [editor    (:source e)
        opening   (:char e)
        closing   (delimiters opening)
        offset    (ui/caret-position editor)
        root-loc  (-> @(ui/attr editor :doc) lang/parse-tree lang/code-zip)
        [loc pos] (lang/location root-loc offset)
        tag       (lang/location-tag loc)]
    (if (ignore? tag)
      (model/insert editor offset (str opening))
      (model/insert editor offset (str opening closing)))
    (ui/caret-position editor (inc offset))))

(def ^:private keymap
  (km/keymap 'lab.plugin.paredit
    :lang :clojure
    {:fn ::balance-delimiter :keystroke "(" :name "Balance parenthesis"}
    {:fn ::balance-delimiter :keystroke "(" :name "Balance parenthesis"}
    {:fn ::balance-delimiter :keystroke "{" :name "Balance curly brackets"}
    {:fn ::balance-delimiter :keystroke "[" :name "Balance square brackets"}
    {:fn ::balance-delimiter :keystroke "\"" :name "Balance double quotes"}))

(plugin/defplugin lab.plugin.paredit
  :keymaps [keymap])
