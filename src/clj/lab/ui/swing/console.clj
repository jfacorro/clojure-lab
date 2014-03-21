(ns lab.ui.swing.console
  (:require [lab.ui.core :as ui]
            [lab.ui.util :refer [definitializations defattributes]])
  (:import lab.ui.swing.JConsole))

(defn console-init [c]
  (let [{:keys [cin cout]} (ui/attr c :conn)]
    (JConsole. cout cin)))

(definitializations
  :console console-init)

(defattributes
  :console
  (:conn [c _ _]))