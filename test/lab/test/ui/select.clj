(ns lab.test.ui.select
  (:require [lab.ui.core :as ui])
  (:use [lab.ui.select :reload true]
        [clojure.test :only [deftest is are run-tests testing]]))

(def attr-spec @#'ui/attr-spec)

(def root (#'lab.ui.core/hiccup->component
            [:window {:id "main" 
                      :attr [:text-area {:id "text1"}]}
              [:label {:id "1"
                       :size [100 100]}]
              [:button {:id "2"
                        :attr [:text-area {:id "text2"}]}
                [:combo {:id "combo"
                         :size [100 200]
                         :attr [:panel [:button]]}
                  [:button]
                  [:text]]]
              [:label {:id "3"
                       :size [100 100]}]
              [:tabs [:tab] [:tab] [:tab]]
              [:panel [:button]]]))

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
      [:content 1 :content 0] [:window :button :combo]
      [:content 1 :content 0 :content 1] [:button :combo :text]
      nil          [:button :label]))

  (testing "Conjunction (and)"
    (are [x y] (= x (select root y))
      []           [[:window :#main]]
      nil          [[:window :label]]
      [:content 0] [[:label :label]]))
  
  (testing "Disjunction (or)"
    (are [x y] (= x (select root y))
      []           #{:window :label}
      nil          #{:x :y}
      [:content 0] #{:label :button})))

(deftest ui-select-all []
  (are [x y] (= x (select-all root y))
    nil nil
    #{[]} []
    #{[:content 0] [:content 2]} :label
    #{[:content 3 :content 0] [:content 3 :content 1] [:content 3 :content 2]} :tab
    #{[]} :window
    #{[] [:content 0] [:content 2]} #{:window :label}
    #{[] [:content 1] [:content 1 :content 0]} #{:window (attr? :attr)}))

(deftest ui-select-all-with-spec []
  (are [x y] (= x (select-all root y attr-spec))
    nil nil
    #{} :does-not-exist
    #{[]} []
    #{[]} :#main
    #{[]} :window
    #{[:content 0] [:content 2]} :label
    #{[:content 3 :content 0] [:content 3 :content 1] [:content 3 :content 2]} :tab
    #{[:attrs :attr]} :#text1
    #{[:content 1 :attrs :attr]} :#text2
    #{[:content 1 :attrs :attr] [:attrs :attr]} :text-area
    #{[:content 1 :content 0 :attrs :attr :content 0] [:content 4 :content 0]} [:panel :button]))
