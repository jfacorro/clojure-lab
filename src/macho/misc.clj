(ns macho.misc)

(defn interned-vars-meta
  "Returns a seq with the metadata of the interned vars 
in the supplied namespace. The argument can be either 
the ns name as symbol or the ns itself."
  [the-ns]
  (->> the-ns ns-interns vals (map meta)))

(defn intern-vars
  "Interns all vars present in the source ns to the target,
taking the value from the var in the source ns."
  [source target]
  (doseq [metadata (interned-vars-meta source)]
    (let [{name-sym :name} metadata]
      (intern target name-sym (intern source name-sym))))
  (the-ns target))