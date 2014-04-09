(ns lab.model.buffer
  "Provides a protocol interface for different text buffer
implementations."
  (:require [clojure.zip :as zip]
            [net.cgrand.parsley :as parsley]
            [net.cgrand.parsley.tree :as tree]
            [lab.model.protocols :as p])
  (:import  [net.cgrand.parsley.tree InnerNode Leaf]))

(defn- node-children [^InnerNode x]
  (if (.c x)
    [(.a x) (.b x) (.c x)]
    [(.a x) (.b x)]))

(defn- to-string
  ([b] (to-string b (StringBuffer.)))
  ([b ^StringBuffer s]
    (loop [z (zip/zipper (partial instance? InnerNode)
                         node-children
                         nil
                         b)]
      (when-not (zip/end? z)
        (when-not (zip/branch? z)
          (.append s (.s ^Leaf (zip/node z))))
        (recur (zip/next z))))
    (.toString s)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Incremental Buffer implementation

(defrecord IncrementalBuffer [buffer]
  p/Text
  (insert [this offset s]
    (assoc this :buffer (parsley/edit buffer offset 0 s)))
  (append [this s]
    (p/insert this (p/length this) s))
  (delete [this start end]
    (assoc this :buffer (parsley/edit buffer start (- end start) "")))
  (length [this]
    (-> buffer :buffer tree/len))
  (text [this]
    (-> buffer :buffer to-string))
  (substring [this start end]
    (-> buffer :buffer to-string (subs start end)))

  p/Parsable
  (parse-tree [this]
    (parsley/parse-tree buffer)))

(defn- build-incremental-buffer
  [lang]
  (let [{:keys [options grammar]}
                lang
        parser  (apply parsley/parser options grammar)]
    (parsley/incremental-buffer parser)))

(def ^:private memoized-build-incremental-buffer
  (memoize build-incremental-buffer))

(defn incremental-buffer
  "Returns an incremental buffer whose content will be parsed using 
the parsing information from the language provided."
  ([lang]
    (incremental-buffer lang ""))
  ([lang s]
    (-> (IncrementalBuffer. (memoized-build-incremental-buffer lang))
        (p/insert 0 s))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; StringBuffer implementation

(extend-type StringBuffer
  p/Text
  (insert [this offset s]
    (.insert this ^int offset ^String s))
  (append [this s]
    (.append this ^String s))
  (delete [this start end]
    (.delete this ^int start ^int end))
  (length [this]
    (.length this))
  (text [this]
    (.toString this))
  (substring [this start end]
    (.substring this start end)))

(defn string-buffer
  "Return a native mutable java StringBuffer instance."
  ([]
    (string-buffer ""))
  ([^String s]
    (StringBuffer. s)))
