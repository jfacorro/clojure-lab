(ns lab.test.core.plugin
  (:require [clojure.test :refer [deftest is run-tests]]
            [lab.core :as lab]
            [lab.core.keymap :as km]
            [lab.core.plugin :refer [defplugin load-plugin! unload-plugin!]]))

;;;;;;;;;;;;;;;;;;;;;
;; Default app atom

(def app (atom lab/default-app))

(defn hooked [x] :hooked)
(defn hook [f x] :hook)

;;;;;;;;;;;;;;;;;;;;;
;; Test

(deftest load-and-unload-plugin
  (is (nil? (:keymap @app )))
  (is (nil? (-> @app :langs :plain-text :keymap (km/find #{"ctrl" "o"}))))
  (is (nil? (:init? @app)))
  (is (= :hooked (hooked 1)))

  (load-plugin! app 'lab.test.core.dummy-plugin)
  (is (:keymap @app))
  (is (-> @app :langs :plain-text :keymap (km/find #{"ctrl" "o"})))
  (is (:init? @app))
  (is (= :hook (hooked 1)))

  (unload-plugin! app 'lab.test.core.dummy-plugin)
  (is (nil? (:keymap @app )))
  (is (nil? (-> @app :langs :plain-text :keymap (km/find #{"ctrl" "o"}))))
  (is (nil? (:init? @app)))
  (is (= :hooked (hooked 1))))

(deftest plugin-definition
  (let [plugin (ns-resolve (the-ns 'lab.test.core.dummy-plugin) 'plugin)]
    (is plugin)
    (is (and (:keymaps @plugin)
             (:hooks @plugin)
             (:init! @plugin)
             (:unload! @plugin)))))
