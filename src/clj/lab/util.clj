(ns lab.util
  (:require [clojure.core.async :as async]
            [clojure.core.async.impl.protocols :as async-protocols]
            [clojure [string :as str]
                     [reflect :as r]
                     [pprint :as p]]))

(defn channel?
  [x]
  (satisfies? async-protocols/Channel x))

(defn timeout-channel
  "Creates a go block that works in two modes :wait and :recieve.
When on ':wait' it blocks execution until a value is recieved
from the channel, it then enters ':recieve' mode until the timeout
wins. Returns a channel that takes the input events."
  [timeout-ms f]
  (let [c (async/chan)]
    (async/go-loop [mode     :wait
                    args     nil]
      (condp = mode
        :wait
          (recur :recieve (async/<! c))
        :recieve
          (let [[_ ch] (async/alts! [c (async/timeout timeout-ms)])]
            (if (= ch c)
              (recur :recieve args)
              (do
                (async/thread (apply f args))
                (recur :wait nil))))))
    c))

(defn keywordize [^String s]
  (-> s
    .toLowerCase
    (.replace " " "-")
    keyword))

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

(defn remove-at
  "Removes the element in the ith position from the given vector."
  [v i]
  (vec (concat (subvec v 0 i) (subvec v (inc i) (count v)))))

(defn index-of
  "Takes a vector and returns the index of x."
  [^clojure.lang.PersistentVector v x]
  (.indexOf v x))

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