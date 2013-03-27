(ns macho
  (:require [macho.ui :as ui])
  (:gen-class [:name macho.main
               :main true]))
;;------------------------------
(defn -main
  "Program startup function."
  []
  (def main (ui/make-main ui/app-name)))
;;------------------------------
