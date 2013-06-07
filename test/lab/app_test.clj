(remove-ns 'lab.app-test)
(ns lab.app-test
  (:use [lab.app :reload true]
        [lab.test :only [->test ->is]]
        [clojure.test :only [deftest is run-tests]]))

(deftest init-app
  (->test
    (init nil)

    (->is not= nil)
    (->is not= nil :workspace)
    (->is not= nil :documents)
    (->is = nil :current-document)))

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
    
    (switch-document "2")
    (is (instance? clojure.lang.Atom :current-document))))

#_(deftest project-operations
  (is false))

#_(deftest workspace-operations
  (is false))

(run-tests)
