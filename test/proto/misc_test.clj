(ns proto.misc-test
  (:use [clojure.test :only [deftest is run-tests]])
  (:use proto.misc))
;----------------------------
;; Create namespace with a single interned var
(remove-ns 'proto.misc-test.ns1var)
(ns proto.misc-test.ns1var)
(def var-one :one)
;; Create namespace with a single interned var
(remove-ns 'proto.misc-test.ns2var)
(ns proto.misc-test.ns2var)
(def var-two :two)
(def var-three :three)
;; Return to test proto.misc-test
(ns proto.misc-test) 
;----------------------------
(deftest interned-vars-meta-test
  (is (= 1 (-> 'proto.misc-test.ns1var interned-vars-meta count)))
  (is (= 2 (-> 'proto.misc-test.ns2var interned-vars-meta count))))
;----------------------------
(deftest interned-vars-test
  (is (not (ns-resolve 'proto.misc-test.ns2var 'var-one)))

  (intern-vars 'proto.misc-test.ns1var 'proto.misc-test.ns2var)
  (is (ns-resolve 'proto.misc-test.ns2var 'var-one))

  ; Clarifies what's going on with all the interning
  (is (= #'proto.misc-test.ns1var/var-one (-> (ns-resolve 'proto.misc-test.ns2var 'var-one) deref)))
  (is (= :one (-> (ns-resolve 'proto.misc-test.ns2var 'var-one) deref deref))))