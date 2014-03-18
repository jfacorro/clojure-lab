(ns lab.test.core.plugin
  (:require [clojure.test :refer [deftest is run-tests testing]]
            [lab.core :as lab]
            [lab.core [keymap :as km]
                      [plugin :refer [defplugin load-plugin! unload-plugin!]]]
            [lab.model.document :as doc]))

;;;;;;;;;;;;;;;;;;;;;
;; Default app atom

(def app (atom lab/default-app))

(defn hooked [x] :hooked)
(defn hook [f x] :hook)

;;;;;;;;;;;;;;;;;;;;;
;; Tests

(deftest load-and-unload-global-plugin
  (is (nil? (:keymap @app)))
  (is (= 0 (count (:plugins @app))))
  (is (nil? (-> @app :langs :plain-text :keymap (km/find #{"ctrl" "o"}))))
  (is (nil? (:init? @app)))
  (is (= :hooked (hooked 1)))

  (load-plugin! app 'lab.test.core.dummy-plugin)
  (is (:keymap @app))
  (is (= 1 (count (:plugins @app))))
  (is (= 'lab.test.core.dummy-plugin (-> @app :plugins first :name)))
  (is (-> @app :langs :plain-text :keymap (km/find #{"ctrl" "o"})))
  (is (:init? @app))
  (is (= :hook (hooked 1)))

  (testing "Load plugin twice, check single loading."
    (load-plugin! app 'lab.test.core.dummy-plugin)
    (is (= 1 (count (:plugins @app)))))

  (unload-plugin! app 'lab.test.core.dummy-plugin)
  (is (nil? (:keymap @app )))
  (is (= 0 (count (:plugins @app))))
  (is (nil? (-> @app :langs :plain-text :keymap (km/find #{"ctrl" "o"}))))
  (is (nil? (:init? @app)))
  (is (= :hooked (hooked 1))))

(deftest load-and-unload-local-plugin
  (swap! app lab/new-document)
  (let [doc (lab/current-document @app)]
    (is (nil? (doc/keymap @doc)))
    (is (= 0 (count (:plugins @doc))))
    (is (nil? (km/find (doc/keymap @doc) #{"ctrl" "o"})))
    (is (nil? (:init? @doc)))
    (is (= :hooked (hooked 1)))

    (load-plugin! app 'lab.test.core.dummy-local-plugin)
    (is (doc/keymap @doc))
    (is (= 1 (count (:plugins @doc))))
    (is (= 'lab.test.core.dummy-local-plugin (-> @doc :plugins first :name)))
    (is (km/find (doc/keymap @doc) #{"ctrl" "o"}))
    (is (:init? @doc))
    (is (= :hook (hooked 1)))

    (testing "Load plugin twice, check single loading."
      (load-plugin! app 'lab.test.core.dummy-local-plugin)
      (is (= 1 (count (:plugins @doc)))))

    (unload-plugin! app 'lab.test.core.dummy-local-plugin)
    (is (nil? (doc/keymap @doc)))
    (is (= 0 (count (:plugins @doc))))
    (is (nil? (km/find (doc/keymap @doc) #{"ctrl" "o"})))
    (is (nil? (:init? @doc)))
    (is (= :hooked (hooked 1)))))

(deftest plugin-definition
  (let [plugin (ns-resolve (the-ns 'lab.test.core.dummy-plugin) 'plugin)]
    (is plugin)
    (is (and (:keymaps @plugin)
             (:hooks @plugin)
             (:init! @plugin)
             (:unload! @plugin)))))
