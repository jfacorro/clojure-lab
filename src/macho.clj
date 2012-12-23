(ns macho
  (:require [macho.ui :as ui]))
;;------------------------------
(defn -main
  "Program startup function."
  []
  (def main (ui/make-main ui/app-name)))
;;------------------------------