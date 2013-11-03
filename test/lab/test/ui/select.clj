(ns lab.test.ui.select
  (:require [lab.ui.core :as ui])
  (:use lab.ui.select
        [clojure.test :only [deftest is are run-tests testing]]))

(def root (#'lab.ui.core/hiccup->component
            [:window {:id "main"}
                     [:label {:id "1" :size [100 100]}]
                     [:button {:id "2"} 
                              [:combo {:id "combo" :size [100 200]}]]
                     [:label {:id "3" :size [100 100]}]
                     [:tabs [:tab] [:tab] [:tab]]]))

(deftest ui-selection []
  (testing "single"
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
      nil :not-found
      [:content 0] (attr? :size)
      nil (attr? :something)
      [:content 0] (attr= :size [100 100])
      [:content 1 :content 0] (attr= :size [100 200])))
      
  (testing "Chained"
    (are [x y] (= x (select root y))
      nil          nil
      []           []
      [:content 0] [:window :label]
      [:content 1] [:#main :#2]
      [:content 1 :content 0] [:button :combo]
      nil          [:button :label]))

  (testing "Conjunction"
    (are [x y] (= x (select root y))
      []           [[:window :#main]]
      nil          [[:window :label]]
      [:content 0] [[:label :label]])))

(deftest ui-select-all []
  (are [x y] (= x (select-all root y))
    nil nil
    #{[]} []
    #{[:content 0] [:content 2]} :label
    #{[:content 3 :content 0] [:content 3 :content 1] [:content 3 :content 2]} :tab
    #{[]} :window))
