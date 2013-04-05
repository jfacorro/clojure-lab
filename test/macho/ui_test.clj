(ns macho.ui-test
  (:refer-clojure :exclude [get set])
  (:use [clojure.test :only [deftest is run-tests]])
  (:require [macho.ui.swing.core :as ui]))

(defn add-to-panel []
  (ui/add (ui/panel) (ui/label "Label")))

(deftest container-protocol
  (is (add-to-panel)))