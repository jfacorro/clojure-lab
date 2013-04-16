(ns lab.test
  (:use [clojure.test :only [is]]))
;---------------------------
(defmacro ->is
  "Rearranges its arguments in order to be
  able to use it in a ->test threading macro."
  [x binop v & f]
  `(clojure.test/is (~binop ~v (-> ~x ~(or f `identity)))))
;---------------------------
(defmacro ->test
  "Threading test macro that allows to use is assert expressions
  in a threading style using the ->is macro."
  [& body]
  (let [->is? (fn [x]
                (and (seq? x) (= '->is (first x))))
        ops   (take-while (complement ->is?) body)
        [[_ & args] & more]
              (drop-while (complement ->is?) body)]
    (if args
      `(let [x# (-> ~@ops)]
         (->is x# ~@args)
         (->test x# ~@(when (seq more) more)))
      `(-> ~@body))))
;---------------------------