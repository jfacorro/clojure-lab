(ns macho.ui-test
  (:use clojure.test)
  ;(:require [macho.ui.swing.core :as ui :reload true])
)

(defn add-to-panel []
  (ui/add (ui/panel)
          (ui/label "Label")))

(deftest container-protocol
  (is (add-to-panel)))
