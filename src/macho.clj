(ns macho
  (:require [macho.ui :as ui])
  (:gen-class [:name macho.main
               :main true]))
;;------------------------------
(def documents
  "Map with current opened documents."
  (atom {}))
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
