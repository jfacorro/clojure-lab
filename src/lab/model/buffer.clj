(ns lab.model.buffer
  "Provides a protocol interface for different text buffer
implementations."
  (:require [clojure.zip :as zip]
            [net.cgrand.parsley :as parsley]
            [net.cgrand.parsley.tree :as tree])
  (:import  [net.cgrand.parsley.tree InnerNode Leaf]))

(defprotocol Buffer
  (insert     [this offset s] "Inserts s in offset.")
  (delete     [this start end] "Delete the contents of the buffer from positions start to end.")
  (length     [this] "Returns the length of the buffer.")
  (text       [this] "Returns the contents of the buffer as a string.")
  (parse-tree [this] "Returns a parse tree with each node being {:tag :tag-kw :content [node*]}"))

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
  Buffer
  (insert [this offset s]
    (assoc this :buffer (parsley/edit buffer offset 0 s)))
  (delete [this start end]
    (assoc this :buffer (parsley/edit buffer start (- end start) "")))
  (length [this]
    (-> buffer :buffer tree/len))
  (text [this]
    (-> buffer :buffer to-string))
  (parse-tree [this]
    (parsley/parse-tree buffer)))

(defn incremental-buffer
  "Returns an incremental buffer whose content will be parsed using 
the parsing information from the language provided."
  ([lang]
    (incremental-buffer lang ""))
  ([lang s]
    (let [{:keys [options grammar]} lang
          parser                    (apply parsley/parser options grammar)]
      (IncrementalBuffer. (-> parser
                              parsley/incremental-buffer
                              (parsley/edit 0 0 s))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; StringBuffer implementation

(extend-protocol Buffer
  StringBuffer
  (insert [this offset s]
    (.insert this ^int offset ^String s))
  (delete [this start end]
    (.delete this ^int start ^int end))
  (length [this]
    (.length this))
  (text [this]
    (.toString this)))

(defn string-buffer
  "Return a native mutable java StringBuffer instance."
  ([]
    (string-buffer ""))
  ([^String s]
    (StringBuffer. s)))
