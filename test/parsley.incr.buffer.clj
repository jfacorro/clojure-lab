(ns macho.parser
  (:require [macho.parser :as mp]
            [clojure.pprint :as pp]
            [clojure.zip :as zip]
            [net.cgrand.parsley :as p]
            [clojure.core.reducers :as r]
            [clojure.string :as str]))
  
(defn make-node [tag content]
  ;(println "<--- make-node --->" tag)
  ;(when (= tag :net.cgrand.parsley/unfinished)
  ;  (pp/pprint content))
  ;(pp/pprint content)
  ;(println "<----------------->")
  {:tag tag :content content})

(defn get-content [tree]
  (let [z (zip/zipper map? :content nil tree)]
    (loop [z z
           s ""]
      (let [node (zip/node z)
            s    (if (string? node) (str s node) s)]
        (if (zip/end? z)
          s
          (recur (zip/next z) s))))))

(defn get-content-pmap [node]
  (letfn [(f [x]
            ;;(prn x)
            (if (string? x)
              x
             (->> x (pmap get-content-pmap))))]
    (if (map? node)
      (apply str (-> node :content f))
      node)))

(defn get-content-fold [node]
  (letfn [(cf
            ([] "")
            ([x y] (str x y)))
          (rf [x y] 
            (if (string? y)
              (cf x y)
              (cf x
                  (r/fold cf rf (:content y)))))]
    (r/fold cf rf (:content node))))

(future
  (let [file   "./src/macho/ui.clj"
        code   (slurp file)
        result (with-out-str
                (doseq [i (range 1 100)]
                  (println "#" i)
                  (let [code   (apply str (repeat i code))
                        parser (apply p/parser (assoc mp/options :make-node make-node) mp/grammar)
                        buf    (-> parser p/incremental-buffer (p/edit 0 0 code))
                        t      (-> buf p/parse-tree)]
                    ;(time (get-content t))
                    (time (get-content-pmap t))
                    (time (get-content-fold t))
                )))
        result (-> result (str/replace #"# (.*)" "$1;")
                          (str/replace #"(?s)\r\n\"Elapsed time: (.*?) msecs\"" "$1;")
                          (str/replace #"\." ","))]
      (println result)
      (spit "./bla.csv" result)
    )
)
