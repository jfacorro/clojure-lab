(ns lab.main
  "Entry point for the whole environment."
  (:require [lab.core :refer [init]])
  (:gen-class))

(def app nil)
;;------------------------------
(defn -main
  "Program startup function."
  [& [config-path & _]]
  (let [app (init config-path)]
    (alter-var-root #'app #(do %2) app)))
;;------------------------------
