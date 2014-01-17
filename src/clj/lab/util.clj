(ns lab.util
  (:require [clojure [string :as str]
                     [reflect :as r]
                     [pprint :as p]]))

(defn keywordize [^String s]
  (-> s
    .toLowerCase
    (.replace " " "-")
    keyword))

(defmacro !
  "Applies f to the atom x using the supplied arguments.
  Convenience macro."
  [f x & args]
  `(swap! ~x ~f ~@args))

(defn interned-vars-meta
  "Returns a seq with the metadata of the interned vars 
  in the supplied namespace. The argument can be either 
  the ns name as symbol or the ns itself."
  [the-ns]
  (->> the-ns ns-interns vals (map meta)))

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

(defn find-limits
  "Returns a lazy sequence of vectors with the
  limits of the matches found in the string 
  by the regex or the Matcher provided."
  ([^String ptrn ^String s]
    (let [m (re-matcher (re-pattern ptrn) s)]
      (find-limits m)))
  ([^java.util.regex.Matcher m]
    (lazy-seq
      (when-let [lim (when (.find m) [(.start m) (.end m)])]
        (cons lim (find-limits m))))))

(defn capitalize-word
  "Turns to upper case the first letter of the string."
  [[x & xs :as s]]
  {:pre [(string? s)]}
  (apply str (str/upper-case x) xs))

(defn capitalize [s]
  "Transforms a string with '-' as word delimiters into
  a CamelCase string."
  (->> (str/split s #"-")      
      (map capitalize-word)
      (apply str)))

(defn property-accesor
  "Returns the symbol whose name is composed of the
  concatenation of the op keyword (:set or :get) and 
  the name of the prop keyword transformed from :camel-case 
  to CamelCase."
  [op prop]
  {:pre [(#{:set :get} op)]}
  (symbol (str (name op) (-> prop name capitalize))))

(defn remove-at
  "Removes the element in the ith position from the given vector."
  [v i]
  (vec (concat (subvec v 0 i) (subvec v (inc i) (count v)))))

;; Reflection

(defmulti class-info (fn [_ info] info))

(defmethod class-info :methods
  [c info]
  (->> (#'r/declared-methods c) (sort-by :name)))

(defmethod class-info :constructors
  [c info]
  (->> (#'r/declared-constructors c) (sort-by :name)))

(defmethod class-info :fields
  [c info]
  (->> (#'r/declared-fields c) (sort-by :name)))

(defn print-info
  ([clazz]
    (doseq [[k v] (r/reflect clazz :ancestors true)]
      (println "-----------------------------")
      (println k)
      (println "-----------------------------")
      (case k
        :ancestors (println v)
        :members   (p/print-table (sort-by #(str (:declaring-class %) (type %)) v))
        :bases     (println v)
        :flags     (println v))))
  ([clazz info]
    (p/print-table (class-info clazz info))))

(defn- access [o mthd]
  {(keyword (:name mthd)) (eval `(. ~o ~(:name mthd)))})

(defn- getter? [mthd]
  (-> mthd :name name (.startsWith "get")))

(defn- no-args? [mthd]
  (-> mthd :parameter-types empty?))

(defn mapize [obj]
  (let [clazz   (class obj)
        members (->> (r/reflect clazz) :members (filter #(and (no-args? %) (getter? %))))]
    (into {} (map (partial access obj) members))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Event Handling

(defn event-handler
  "Builds a function that swap!s the x using
f, which should take a value and an event."
  ([f]
    (fn [x evt]
      (assert (instance? clojure.lang.IRef x) (str "x should be a reference. f: " f " - event: " (class evt)))
      (f x evt)))
  ([f x]
    (partial (event-handler f) x)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; break

(defn readr [locals prompt exit-code]
  (let [input (clojure.main/repl-read prompt exit-code)]
    (if (= input :quit) 
      exit-code
      (do
        (when (= input :locals)
          (println locals))
        input))))

(defn contextual-eval [ctx expr]
  (eval
    `(let [~@(mapcat (fn [[k v]] [k `'~v]) ctx)]
      ~expr)))

(defmacro local-context []
  (let [symbols (keys &env)]
    `(zipmap '~symbols (list ~@symbols))))

(defmacro break []
  `(clojure.main/repl
    :prompt #(print "debug=> ")
    :read (partial readr (local-context))
    :eval (partial contextual-eval (local-context))))