(ns proto.main
  (:require [proto.ui :as ui])
  (:gen-class [:name proto.main
               :main true]))
;;------------------------------
(def main
  "Main UI window."
  (atom nil))
;;------------------------------
(defn -main
  "Program startup function."
  []
  (reset! main (ui/make-main ui/app-name)))
;;------------------------------
