(remove-ns 'lab.app-test)
(ns lab.app-test
  (:use [lab.app :reload true]
        [lab.test :only [->test ->is]]
        [clojure.test :only [deftest is run-tests use-fixtures]])
  (:require [clojure.java.io :as io]))
;;------------------------------------
(def file-content "Temp file, should be deleted.")
(def tmp-files ["./tmp" "../tmp"])
;;------------------------------------
(defn delete-file [path]
  (when (-> path io/file .exists)
    (io/delete-file path)))
;;------------------------------------
(defn create-file [path]
  (spit path file-content))
;;------------------------------------
(defn temp-document-config
  [f]
  (try
    (dorun (map create-file tmp-files))
    (f)
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

    (open-document)
    (->is = 1 (comp count :documents))
    (->is not= nil :current-document)
    (->is = "Untitled" (comp :name deref :current-document))
    
    (open-document)
    (->is = 2 (comp count :documents))
    (->is = "Untitled-0" (comp :name deref :current-document))
    
    (switch-document "Untitled")
    (->is = "Untitled" (comp :name deref :current-document))
    
    (close-document "Untitled")
    (->is = 1 (comp count :documents))
    (->is = nil (comp :current-document))
    
    (open-document "./tmp")
    (->is = 2 (comp count :documents))
    (->is not= nil (comp :current-document))
    (->is = "tmp" (comp :name deref :current-document))
    
    (open-document "../tmp")
    (->is = 3 (comp count :documents))
    (->is not= nil (comp :current-document))
    (->is = "tmp-0" (comp :name deref :current-document))
    
    (switch-document "Untitled-0")
    (is (instance? clojure.lang.Atom :current-document))))
;;------------------------------------
#_(deftest project-operations
  (is false))
;;------------------------------------
#_(deftest workspace-operations
  (is false))
;;------------------------------------
