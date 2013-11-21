(ns lab.test.ui
  (:refer-clojure :exclude [find remove])
  (:use lab.ui.core
        [clojure.test :only [deftest is are run-tests testing]]))

(def tab1 [:tab {:id "2"}])
(def tab2 [:tab {:id "3"}])

(def ts [:tabs {:id "0"} tab1 tab2])

(def tr [:tree {:id "1"}])

(def ui-test (#'lab.ui.core/hiccup->component [:window ts tr]))

(deftest find-in-ui
  (testing "Find by id"
    (are [x y] (= x (-> ui-test (find y) (attr :id)))
      "0"   :#0  
      "2"   :#2
      "1"   :#1
      nil  :#9)))

(deftest update-ui
  (testing "Udpate by id"
    (is (= (assoc-in ui-test [:content 0 :content 1 :attrs :bla] 1)
           (update ui-test :#3 assoc-in [:attrs :bla] 1)))))
