(ns macho.document-test
  (:use [clojure.test :only [deftest is run-tests]]
        macho.document)
  (:require [clojure.java.io :as io]))
;---------------------------
(def ^:dynamic *untitled* "New document")
(def tmp-file "./tmp")
(defn new-document []
  (make-document *untitled*))
;---------------------------
(deftest document-creation
  (is (thrown? Error (make-document nil)))
  (is (= "" (text (new-document)))))
;---------------------------
(deftest document-manipulation  
  (let [content  "Temp file, should be deleted."
        end      " Oh yes, it will!"
        middle   "Do you think so?"
        len      (count content)
        _        (spit tmp-file content) ; create temp file
        doc      (bind (new-document) tmp-file)]
    (is (= content
           (text doc)))
    (is (= (str content end)
           (-> doc (append end) text)))
    (is (= (str content middle end) 
           (-> doc (insert len middle) text)))
    (is (= (str content end)
           (-> doc (delete len (+ len (count middle))) text)))
    ; delete temp file
    (io/delete-file tmp-file)))
;---------------------------
(deftest bind-non-existing-file
    (is (thrown? java.io.IOException (bind (new-document) tmp-file))))
;---------------------------
(run-tests)
