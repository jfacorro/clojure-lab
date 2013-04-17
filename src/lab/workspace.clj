(ns lab.workspace)

(defn add-project [{projects :projects :as ws} p]
  (assoc ws :projects (assoc projects (:id p) p)))

(defn get-project [{projects :projects :as ws} id]
  (-> projects id))

(defn workspace [& xs]
  {})