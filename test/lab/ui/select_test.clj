(ns lab.ui.select-test
  (:use lab.ui.select
        [clojure.test :only [deftest is are run-tests testing]]))

(def root {:tag :window
           :attrs {:-id "main"}
           :content [{:tag :label 
                      :attrs {:-id "1"}}
                     {:tag :button 
                      :attrs {:-id "2"} 
                      :content [{:tag :combo 
                                 :attrs{:-id "combo"}}]}]})

(deftest ui-selection []
  (testing "Single"
    (are [x y] (= x (select root y))
      [] :window
      [] :#main
      [:content 0] :label
      [:content 0] :#1
      [:content 1] :button
      [:content 1] :#2
      [:content 1 :content 0] :#combo
      [:content 1 :content 0] :combo
      nil :#not-found
      nil :not-found))
      
  (testing "Chained"
    (are [x y] (= x (select root y))
      [:content 0] [:window :label]
      [:content 1] [:#main :#2]
      [:content 1 :content 0] [:button :combo]))

  (testing "Conjunction"
    (are [x y] (= x (select root y))
      []  [[:window :#main]]
      nil [[:window :label]]
      )))

(run-tests)
