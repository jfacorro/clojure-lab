(ns macho.buffer
  "Provides a protocol interface for different text buffer 
  implementations."
  (:require [net.cgrand.parsley.tree :as t]))

(defprotocol Buffer
  (insert [this offset s] "Inserts s in offset.")
  (delete [this start end] "Delete the contents of the buffer from positions start to end.")
  (length [this] "Returns the length of the buffer.")
  (text [this] "Returns the contents of the buffer as a string."))

(defn to-string 
  ([b] (to-string b (StringBuffer.)))
  ([b s]
    (cond (instance? net.cgrand.parsley.tree.Leaf b) 
            (.append s (.s b))
          :else
            (doall
              (map #(to-string % s) (if (.c b) [(.a b) (.b b) (.c b)] [(.a b) (.b b)]))))
    (.toString s)))

; Incremental Buffer implementation
(defrecord IncrementalBuffer [buffer]
  Buffer
  (insert [this offset s]
    (assoc this :buffer (t/edit buffer offset 0 s)))
  (delete [this start end]
    (assoc this :buffer (t/edit buffer start (- end start) "")))
  (length [this]
    (t/len buffer))
  (text [this]
    (to-string buffer)))

(def incr-buff-ops {:unit identity 
                    :plus str
                    :chunk #(.split ^String % "(?<=\n)")
                    :left #(subs %1 0 %2)
                    :right subs 
                    :cat str})
      
(defn incremental-buffer
  "Returns an incremental buffer, implemented with 2-3 trees."
  ([]
    (incremental-buffer ""))
  ([s] 
    (IncrementalBuffer. (t/buffer incr-buff-ops s))))

; String Buffer implementation
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
