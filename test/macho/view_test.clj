(ns macho.view-test
  (:use [clojure.test :only [deftest is run-tests]]
        [macho.view]))
;----------------------------------------
; defview* macro
;----------------------------------------
(defn create-with-defview* []
  (defview*
    {:create (fn [] 1)
     :to-str (fn [x] (str x))}))

(deftest defview*-fail
  (is (thrown? Exception (defview* nil)))
  (is (thrown? Exception (defview* {:create nil :this nil})))
  (is (thrown? Exception (defview* {:no-create nil}))))

(deftest defview*-win
  (is (create-with-defview*))
  (is (= "1/2" ((create-with-defview*) :to-str 1/2))))

(deftest nonexisting-op-error
  (is (thrown? Error ((create-with-defview*) :no-op))))

(deftest defview-fail
  (is (thrown? Exception (defview view)))
  (is (thrown? Exception (defview view
                           (create [] 1)
                           (this [] nil))))
  (is (thrown? Exception (defview view
                           (no-create [] 1)))))
;----------------------------------------
; defview macro
;----------------------------------------
(defview a-view
  (create [] 1)
  (to-str [] (str this)))

(deftest defview-win
  (is (intern 'macho.view-test 'a-view)
  (is (= "1" (a-view :to-str)))))
;----------------------------------------
(run-tests)
