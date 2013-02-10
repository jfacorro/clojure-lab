(ns macho.misc)

(defn interned-vars [ns-name]
  (->> ns-name ns-interns vals (map meta)))

(defn print-vars [ns-name]
  (let [iv (interned-vars ns-name)]
    (doseq [{ns :ns name :name} iv] 
      (println "--------------\n"
               "name: "name "\n"
               "ns: "ns "\n" 
               "var: " (intern ns name) "\n"
               "var value: " @(intern ns name) "\n"
               "meta var: " (meta (intern ns name)) "\n"
               "meta var value: " (meta @(intern ns name))))))

(defn intern-vars [from-ns to-ns]
  (doseq [metadata (interned-vars from-ns)]
    (let [{ns :ns name-sym :name protocol :protocol} metadata]
      (intern to-ns name-sym (intern from-ns name-sym)))))

(comment 
  (print-vars *ns*)
  (println "------")
  (intern-vars 'macho.ui.protocols *ns*)
  (print-vars *ns*)
)