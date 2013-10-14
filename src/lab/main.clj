(ns lab.main
  "Entry point for the whole environment."
  (:require [lab.app :as app])
  (:gen-class [:name lab.main
               :main true]))

(def app nil)
;;------------------------------
(defn -main
  "Program startup function."
  [config-path & _]
  (let [app (app/init config-path)]
    (alter-var-root #'app #(do %2) app)))
;;------------------------------
