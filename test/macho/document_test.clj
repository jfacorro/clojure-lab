(remove-ns 'macho.document-test)
(ns macho.document-test
  (:use [clojure.test :only [deftest is run-tests]]
        macho.document)
  (:require [clojure.java.io :as io]))
;---------------------------
(def ^:dynamic *untitled* "New document")
;---------------------------
(deftest document-creation
  (is (thrown? Error (make-document nil)))
  (is (= "" (-> (make-document *untitled*) text))))
;---------------------------
(deftest document-manipulation  
  (let [tmp-file "./tmp"
        content  "Temp file, should be deleted."
        len      (count content)
        end      " Oh yes, it will!"
        middle   "Do you think so?"
        _        (spit tmp-file content) ; create temp file
        doc      (-> (make-document *untitled*) (bind tmp-file))]
    (is (= content
           (text doc)))
    (is (= (str content end)
           (-> doc (append end) text)))
    (is (= (str content middle end) 
           (-> doc (insert len middle) text)))
    (is (= (str content end)
           (-> doc (delete len (+ len (count middle))) text)))

    (io/delete-file tmp-file) ; delete temp file
    (is (thrown? java.io.IOException (-> (make-document *untitled*) (bind tmp-file))))))
;---------------------------
(run-tests)
