(ns lab.ui-test
  (:refer-clojure :exclude [find])
  (:use lab.ui.core
        [clojure.test :only [deftest is are run-tests testing]]))

(def tab1 (hiccup->map [:tab {:id "2"}]))
(def tab2 (hiccup->map [:tab {:id "3"}]))

(def ts (hiccup->map [:tabs {:id "0"} tab1 tab2]))

(def tr (hiccup->map [:tree {:id "1"}]))

(def ui-test (hiccup->map [:window ts tr]))

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
