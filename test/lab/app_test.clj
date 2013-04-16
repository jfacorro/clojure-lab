(remove-ns 'lab.app-test)
(ns lab.app-test
  (:use [lab.app :reload true]
        [lab.util :only [!]]
        [clojure.test :only [deftest is run-tests]]))

(defn create-app []
  (-> {} init atom))

(deftest init-app
  (let [app (create-app)]
    (is @app)
    (is (:workspace @app))
    (is (:documents @app))
    (is (nil? (:current-document @app)))))

(deftest document-operations
  (let [app (create-app)]
    (! open-document app "1")
    (is (= 1 (-> app deref :documents count)))
    (is (:current-document @app))
    (is (= "1" (-> app deref :current-document deref :path)))

    (! open-document app "2")
    (is (= 2 (-> app deref :documents count)))
    (is (= "2" (-> app deref :current-document deref :path)))
    
    (! switch-document app (hash "1"))
    (is (= "1" (-> app deref :current-document deref :path)))
    
    (! close-document app (hash "1"))
    (is (= 1 (-> app deref :documents count)))
    (is (= nil (-> app deref :current-document)))
    
    (! switch-document app (hash "2"))
    (is (instance? clojure.lang.Atom (current-document app)))))

(deftest project-operations
  (is false))

(deftest workspace-operations
  (is false))

