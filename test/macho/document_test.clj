(ns macho.document-test
  (:use [clojure.test :only [deftest is run-tests]]
        macho.document)
  (:require [clojure.java.io :as io]))
;---------------------------
(defmacro ->is [x binop v & f]
  `(do
     (is (~binop ~v (-> ~x ~(or f `identity))))
     ~x))
;---------------------------
(defmacro ->test
  "Threading test macro that allows to use is assert expressions
  in a threading style using the ->is macro."
  [& body]
  (let [[ops [[is-expr & args]] & more] (partition-by #(and (seq? %) (= '->is (first %))) body)
        more (mapcat identity more)]
    (if (= is-expr '->is)
      `(let [x# (-> ~@ops)]
         (->is x# ~@args)
         (->test x# ~@(when (seq more) more)))
      `(-> ~@body))))
;---------------------------
(def ^:dynamic *untitled* "New document")
(def tmp-file "./tmp")
(defn new-document []
  (document *untitled*))
;---------------------------
(deftest document-creation
  (is (thrown? Error (document nil)))
  (is (= "" (text (new-document)))))
;---------------------------
(deftest document-manipulation  
  (let [content  "Temp file, should be deleted."
        end      " Oh yes, it will!"
        middle   "Do you think so?"
        len      (count content)]
    (spit tmp-file content) ; create temp file
    (->test
        (new-document)
        (bind tmp-file)
        (->is = content text)
        (->is = false modified?)

        (append end)
        (->is = true modified?)
        (->is = (str content end) text)

        (insert len middle)
        (->is = (str content middle end) text)

        (delete len (+ len (count middle)))
        (->is = (str content end) text))
    ; delete temp file
    (io/delete-file tmp-file)))
;---------------------------
(deftest bind-non-existing-file
    (is (thrown? java.io.IOException (bind (new-document) tmp-file))))
;---------------------------
