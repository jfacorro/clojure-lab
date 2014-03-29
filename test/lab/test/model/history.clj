(ns lab.test.model.history
  (:refer-clojure :exclude [name replace])
  (:use [clojure.test :only [deftest testing is run-tests]]
        [lab.test :only [->test ->is]]
        [lab.model.history :only [history add current rewind forward]]))

(testing "History operations"
  (->test (history)
    (add :a)
    (->is = :a current)
    (add :b)
    (add :c)
    (->is = :c current)
    (add :d)
    rewind
    (->is = :c current)
    rewind
    (->is = :b current)
    rewind
    (->is = :a current)
    rewind
    (->is = nil current)
    forward
    forward
    (->is = :b current)
    forward
    forward
    (->is = :d current)))
