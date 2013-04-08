(ns macho.misc-test
  (:use [clojure.test :only [deftest is run-tests]])
  (:use macho.misc))
;----------------------------
;; Create namespace with a single interned var
(remove-ns 'macho.misc-test.ns1var)
(ns macho.misc-test.ns1var)
(def var-one :one)
;; Create namespace with a single interned var
(remove-ns 'macho.misc-test.ns2var)
(ns macho.misc-test.ns2var)
(def var-two :two)
(def var-three :three)
;; Return to test macho.misc-test
(ns macho.misc-test) 
;----------------------------
(deftest interned-vars-meta-test
  (is (= 1 (-> 'macho.misc-test.ns1var interned-vars-meta count)))
  (is (= 2 (-> 'macho.misc-test.ns2var interned-vars-meta count))))
;----------------------------
(deftest interned-vars-test
  (is (not (ns-resolve 'macho.misc-test.ns2var 'var-one)))

  (intern-vars 'macho.misc-test.ns1var 'macho.misc-test.ns2var)
  (is (ns-resolve 'macho.misc-test.ns2var 'var-one))

  ; Clarifies what's going on with all the interning
  (is (= #'macho.misc-test.ns1var/var-one (-> (ns-resolve 'macho.misc-test.ns2var 'var-one) deref)))
  (is (= :one (-> (ns-resolve 'macho.misc-test.ns2var 'var-one) deref deref))))