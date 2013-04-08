(ns macho.misc)
;---------------------------------
(defn interned-vars-meta
  "Returns a seq with the metadata of the interned vars 
in the supplied namespace. The argument can be either 
the ns name as symbol or the ns itself."
  [the-ns]
  (->> the-ns ns-interns vals (map meta)))
;---------------------------------
(defn intern-vars
  "Interns all vars present in the source ns to the target,
taking the value from the var in the source ns."
  ([source]
    (intern-vars source *ns*))
  ([source target]
    (doseq [metadata (interned-vars-meta source)]
      (let [{name-sym :name} metadata]
        (intern target name-sym (intern source name-sym))))
    (the-ns target)))
;---------------------------------
(defn find-limits
  "Returns a lazy sequence of vectors with the
limits of the matches found in the string 
by the regex or the Matcher provided."
  ([^String ptrn ^String s]
    (let [m (re-matcher (re-pattern ptrn) s)]
      (find-limits m)))
  ([m]
    (lazy-seq
      (when-let [lim (when (.find m) [(.start m) (.end m)])]
        (cons lim (find-limits m))))))