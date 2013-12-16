(ns lab.model.buffer
  "Provides a protocol interface for different text buffer
implementations."
  (:require [net.cgrand.parsley :as parsley]
            [net.cgrand.parsley.tree :as tree]))

(defprotocol Buffer
  (insert     [this offset s] "Inserts s in offset.")
  (delete     [this start end] "Delete the contents of the buffer from positions start to end.")
  (length     [this] "Returns the length of the buffer.")
  (text       [this] "Returns the contents of the buffer as a string.")
  (parse-tree [this] "Returns a parse tree with each node being {:tag :tag-kw :content [node*]}"))

(defn to-string
  ([b] (to-string b (StringBuffer.)))
  ([b s]
    (cond (instance? net.cgrand.parsley.tree.Leaf b) 
            (.append s (.s b))
          :else
            (doall
              (map #(to-string % s) (if (.c b) [(.a b) (.b b) (.c b)] [(.a b) (.b b)]))))
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

(extend-type StringBuffer
  Buffer
  (insert [this offset s]
    (.insert this offset s))
  (delete [this start end]
    (.delete this start end))
  (length [this]
    (.length this))
  (text [this]
    (.toString this)))

(defn string-buffer
  "Return a native mutable java StringBuffer instance."
  ([]
    (string-buffer ""))
  ([s]
    (StringBuffer. s)))
