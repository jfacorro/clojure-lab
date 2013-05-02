(ns lab.ui-test
  (:use lab.ui.core
        [clojure.test :only [deftest is are run-tests testing]]))

(def ui-test (window [(tabs :ui/id :0 :content [(tab :ui/id 2) (tab :ui/id 5)])
                      (tree :ui/id :1)]))

(deftest find-functions
  (testing "Find path by id"
    (are [x y] (= x y)
      [:content 0] (find-path-by-id ui-test :0)
      [:content 0 :content 0] (find-path-by-id ui-test 2)
      [:content 0 :content 1] (find-path-by-id ui-test 5)
      nil (find-path-by-id ui-test :9)))
  (testing "Find by id"
    (is (not= nil (find-by-id ui-test :1)))
    (is (= nil (find-by-id ui-test :3))))
  (testing "Udpate by id"
    (is (= (assoc-in ui-test [:content 0 :content 1 :attrs :bla] 1)
           (update-by-id ui-test 5 #(assoc-in % [:attrs :bla] 1))))))

(run-tests)