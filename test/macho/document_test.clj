(ns macho.document-test
  (:refer-clojure :exclude [name replace])
  (:use [clojure.test :only [deftest is run-tests]]
        macho.document)
  (:require [clojure.java.io :as io]))
;---------------------------
(defmacro ->is
  "Rearranges its arguments in order to be
  able to use it in a ->test threading macro."
  [x binop v & f]
  `(is (~binop ~v (-> ~x ~(or f `identity)))))
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
        ; Check new document properties
        (new-document)
        (->is = false modified?)
        (->is = "" text)
        (->is = 0 length)
        (->is = *untitled* name)
        ; Bind the document to a file
        (bind tmp-file)
        (->is = false modified?)
        (->is = content text)
        (->is = "tmp" name)
        ; Append text to the document
        (append end)
        (->is = true modified?)
        (->is = (str content end) text)
        ; Insert text in the middle
        (insert len middle)
        (->is = (str content middle end) text)
        ; Delete text from the middle
        (delete len (+ len (count middle)))
        (->is = (str content end) text))
    ; delete temp file
    (io/delete-file tmp-file)))
;---------------------------
(deftest bind-non-existing-file
    (is (thrown? java.io.IOException (bind (new-document) tmp-file))))
;---------------------------
(deftest search-and-replace
  ; Search
  (->test
    (new-document)
    (append "abc\nabc\nd")
    (search "b")
    (->is = [[1 2] [5 6]]))
  ; Replace
  (->test
    (new-document)
    (append "abc\nabc\nd")
    (replace "b" "1")
    (->is = "a1c\na1c\nd" text)))
;---------------------------
