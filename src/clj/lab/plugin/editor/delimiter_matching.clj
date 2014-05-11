(ns lab.plugin.editor.delimiter-matching
  (:require [clojure.core.async :as async]
            [lab.ui.core :as ui]
            [lab.model.document :as doc]
            [lab.core [plugin :as plugin]
                      [lang :as lang]
                      [main :as main]]))

(defn- check-for-delimiters [e highlights]
  (let [editor    (:source e)
        doc       (ui/attr editor :doc)
        lang      (:lang @doc)
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
      (when-let [e (async/<! ch)]
        (check-for-delimiters e highlights)
        (recur)))
    ch))

(defn- text-editor-init [editor]
  (let [ch     (find-matching-delimiter)]
    (-> editor
      (ui/update-attr :stuff assoc ::listener ch)
      (ui/listen :caret ch))))

(defn- text-editor-unload [editor]
  (let [ch (::listener (ui/stuff editor))]
    (-> editor
      (ui/update-attr :stuff dissoc ::listener)
      (ui/ignore :caret ch))))

(defn init! [app]
  (let [ui     (:ui @app)
        editor (main/current-text-editor @ui)
        id     (ui/attr editor :id)]
    (ui/update! ui (ui/id= id) text-editor-init)))

(defn unload! [app]
  (let [ui (:ui @app)
        id (ui/attr (main/current-text-editor @ui) :id)]
    (ui/update! ui (ui/id= id) text-editor-unload)))

(plugin/defplugin lab.plugin.editor.delimiter-matching
  :type    :local
  :init!   init!
  :unload! unload!)
