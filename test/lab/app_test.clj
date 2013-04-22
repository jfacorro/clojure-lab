(remove-ns 'lab.app-test)
(ns lab.app-test
  (:use [lab.model.app :reload true]
        [lab.test :only [->test ->is]]
        [clojure.test :only [deftest is run-tests]]))

(def config {})

(deftest init-app
  (->test
    (init config)

    (->is not= nil)
    (->is not= nil :workspace)
    (->is not= nil :documents)
    (->is = nil :current-document)))

(deftest document-operations
  (->test
    (init config)

    (open-document "1")
    (->is = 1 (comp count :documents))
    (->is not= nil :current-document)
    (->is = "1" (comp :path deref :current-document))
    
    (open-document "2")
    (->is = 2 (comp count :documents))
    (->is = "2" (comp :path deref :current-document))
    
    (switch-document (hash "1"))
    (->is = "1" (comp :path deref :current-document))
    
    (close-document (hash "1"))
    (->is = 1 (comp count :documents))
    (->is = nil (comp :current-document))
    
    (switch-document (hash "2"))
    (is (instance? clojure.lang.Atom :current-document))))

(deftest project-operations
  (is false))

(deftest workspace-operations
  (is false))
