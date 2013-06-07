(ns lab.app-test
  (:use [lab.app :reload true]
        [lab.test :only [->test ->is]]
        [clojure.test :only [deftest is run-tests use-fixtures]])
  (:require [clojure.java.io :as io]
            [lab.model.document :as doc]))
;;------------------------------------
(def tmp-files ["./tmp" "../tmp"])
;;------------------------------------
(defn delete-file
  "Deletes a file if it exists."
  [path]
  (when (-> path io/file .exists)
    (io/delete-file path)))
;;------------------------------------
(defn create-file
  "Creates a file with the supplied content."
  [content path]
  (spit path content))
;;------------------------------------
(defn temp-document-config
  "Fixture that creates some temp files and sets
  the new document counter to 0."
  [f]
  (try
    (binding [doc/*untitled-count* (atom 0)]
      (dorun (map (partial create-file "Temp file, should be deleted.") tmp-files))
      (f))
    (finally
      (dorun (map delete-file tmp-files)))))
;;------------------------------------
(use-fixtures :once temp-document-config)
;;------------------------------------
(deftest init-app
  (->test
    (init nil)

    (->is not= nil)
    (->is not= nil :workspace)
    (->is not= nil :documents)
    (->is = nil :current-document)))
;;------------------------------------
(deftest document-operations
  (->test
    (init nil)

    (new-document)
    (->is = 1 (comp count :documents))
    (->is not= nil :current-document)
    (->is = "Untitled 1" (comp :name deref :current-document))
    
    (new-document)
    (->is = 2 (comp count :documents))
    (->is = "Untitled 2" (comp :name deref :current-document))
    
    (as-> x (switch-document x (find-doc-by-name x "Untitled 1")))
    (->is = "Untitled 1" (comp :name deref :current-document))
    
    (as-> x (close-document x (find-doc-by-name x "Untitled 1")))
    (->is = 1 (comp count :documents))
    (->is = nil (comp :current-document))
    
    (open-document "./tmp")
    (->is = 2 (comp count :documents))
    (->is not= nil (comp :current-document))
    (->is = "tmp" (comp :name deref :current-document))

    (open-document "../tmp")
    (->is = 3 (comp count :documents))
    (->is not= nil (comp :current-document))
    (->is = "tmp" (comp :name deref :current-document))

    ; Open the same file with different paths
    ; and check it's still 3 documents.
    (open-document ".././tmp")
    (->is = 3 (comp count :documents))
    
    (open-document "./../tmp")
    (->is = 3 (comp count :documents))
    
    (as-> x (switch-document x (find-doc-by-name x "tmp")))
    (is (instance? clojure.lang.Atom :current-document))))
;;------------------------------------
#_(deftest project-operations
  (is false))
;;------------------------------------
#_(deftest workspace-operations
  (is false))
;;------------------------------------
