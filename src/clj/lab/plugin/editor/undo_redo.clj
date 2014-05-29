(ns lab.plugin.editor.undo-redo
  (:require [lab.core [main :refer [current-text-editor]]
                      [keymap :as km]
                      [plugin :refer [defplugin]]]
            [lab.model.document :as doc]
            [lab.ui.core :as ui]))

(defn undo-redo! [e f]
  (let [app    (:app e)
        ui     (:ui @app)
        editor (current-text-editor @ui)]
    (when editor
      (let [id     (ui/attr editor :id)
            doc    (ui/attr editor :doc)
            hist   (:history @doc)]
        (swap! doc f)
        ;; TODO: Fix this abominable scheme for undo/redo
        (doc/no-op
          (let [[editor hist] (f editor hist)]
            (ui/update! ui (ui/id= id) (constantly editor))))))))

(defn redo! [e]
  (undo-redo! e doc/redo))

(defn undo! [e]
  (undo-redo! e doc/undo))

(def ^:private keymaps
  [(km/keymap (ns-name *ns*)
     :global
     {:category "Edit", :name "Undo", :fn ::undo!, :keystroke "ctrl z"}
     {:category "Edit", :name "Redo", :fn ::redo!, :keystroke "ctrl y"})])

(defplugin lab.core.main
  "Provides the global commands for undo and redo operations."
  :type     :global
  :keymaps  keymaps)
