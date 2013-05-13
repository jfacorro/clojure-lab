(ns lab.main
  (:require [lab.app :as app])
  (:gen-class [:name lab.main
               :main true]))
;;------------------------------
(defn -main
  "Program startup function."
  [config-path & _]
  (app/init config-path))
;;------------------------------
