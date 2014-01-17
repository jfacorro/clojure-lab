(ns proto.main
  (:require [proto.ui :as ui])
  (:gen-class [:name proto.main
               :main true]))
;;------------------------------
(def main
  "Main UI window."
  nil)
;;------------------------------
(defn -main
  "Program startup function."
  []
  (def main (ui/make-main)))
;;------------------------------
