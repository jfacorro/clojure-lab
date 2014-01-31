(ns lab.plugin.delimiter-matching
  (:require [clojure.core.async :as async]
            [lab.ui.core :as ui]
            [lab.model.document :as doc]
            [lab.core [plugin :as plugin]
                      [lang :as lang]
                      [keymap :as km]]))

(defn- check-for-delimiters [app e highlights]
  (let [editor    (:source e)
        doc       (ui/attr editor :doc)
        lang      (doc/lang @doc)
        pos       (:position e)
        delimiter-match (:delimiter-match lang)
        add-hl    #(ui/add-highlight editor % (inc %) 0x888888)]
    (when delimiter-match
      (ui/action
        (doseq [x @highlights]
          (swap! highlights disj)
          (ui/remove-highlight editor x))
        (swap! highlights into (mapv add-hl (delimiter-match @doc pos)))))))

(defn- find-matching-delimiter []
  (let [ch         (async/chan)
        highlights (atom #{})]
    (async/go-loop []
      (let [[app e] (async/<! ch)]
        (when e
          (check-for-delimiters app e highlights)
          (recur))))
    ch))

(defn- text-editor-hook [f doc]
  (let [editor (f doc)
        ch     (find-matching-delimiter)]
    (ui/attr editor :on-caret #(async/put! ch %&))))

(def ^:private hooks
  {#'lab.ui.templates/text-editor #'text-editor-hook})

(plugin/defplugin lab.plugin.delimiter-matching
  :hooks hooks)

