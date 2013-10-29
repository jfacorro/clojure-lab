(ns lab.ui-test
  (:refer-clojure :exclude [find remove])
  (:use lab.ui.core
        [clojure.test :only [deftest is are run-tests testing]]))

(def tab1 (hiccup->component [:tab {:id "2"}]))
(def tab2 (hiccup->component [:tab {:id "3"}]))

(def ts (hiccup->component [:tabs {:id "0"} tab1 tab2]))

(def tr (hiccup->component [:tree {:id "1"}]))

(def ui-test (hiccup->component [:window ts tr]))

(deftest find-in-ui
  (testing "Find by id"
    (are [x y] (= x (find ui-test y))
      ts   :#0
      tab1 :#2
      tr   :#1
      nil  :#9)))

(deftest update-ui
  (testing "Udpate by id"
    (is (= (assoc-in ui-test [:content 0 :content 1 :attrs :bla] 1)
           (update ui-test :#3 assoc-in [:attrs :bla] 1)))))
