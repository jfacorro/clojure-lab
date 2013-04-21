(ns macho
  (:require [macho.ui :as ui])
  (:gen-class [:name macho.main
               :main true]))
;;------------------------------
(def main
  "Main UI window.")
;;------------------------------
(defn -main
  "Program startup function."
  []
  (alter-var-root #'main #(do % (ui/make-main ui/app-name))))
;;------------------------------
