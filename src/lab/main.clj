(ns lab.main
  (:require [lab.app :as app]
            [lab.ui :as ui]
            [clojure.java.io :as io])
  (:gen-class [:name lab.main
               :main true]))
;;------------------------------
(def default-configuration
  {:name "Clojure Lab"
   :extension-folder "extensions"
   :languages-folder "languages"})
;;------------------------------
(defn load-config
  ([]
    (load-config "./lab.config"))
  ([path]
    (let [config-file (io/file path)]
      (merge default-configuration
             (or (and (.exists config-file)
                      (load-string (slurp path)))
                  {})))))
;;------------------------------
(defn -main
  "Program startup function."
  []
  (-> (load-config)
      app/init
      ui/init))
;;------------------------------
