(ns macho.ui-test
  (:refer-clojure :exclude [get set])
  (:use [clojure.test :only [deftest is run-tests]]
        macho.ui.swing.core))

(defn add-to-panel []
  (add (panel)
          (label "Label")))

(deftest container-protocol
  (is (add-to-panel)))
