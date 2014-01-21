(ns lab.model.history
  "API to create and manipulate a history of operations."
  (:refer-clojure :exclude [empty empty?])
  (:require [clojure.core :as core]))

(defprotocol Bijection
  (direct [this] "Returns a direct monadic function of this operation.")
  (inverse [this] "Returns an inverse monadic function of this operation."))

(def ^{:dynamic true :private true} *save-in-history*
  "Indicates if record-operations function should add
  operations to the history. Should be set to false when
  grouping operations to save in the history."
  true)

(defmacro with-no-history
  "Disable saving to history for the operations done in
  the &body."
  [& body]
  `(binding [*save-in-history* false]
    ~@body))

(defmacro with-history
  "By default operations that modify a document are 
  saved in the history, but in case saving to the history 
  was disabled upstream, you might like to be make 
  sure that operations are being saved."
  [& body]
  `(binding [*save-in-history* true]
    ~@body))


(defn history
  "Creates a history that mantains two stacks (past and future)."
  ([]
    (history [] []))
  ([past]
    (history past []))
  ([past fut]
    {:past   past
     :future fut}))

(defn current
  "Returns the last operation added."
  [h]
  (peek (:past h)))

(defn forward
  "Moves an operation from the future to the past."
  [h]
  (if-let [x (-> h :future peek)]
    (-> h (update-in [:past] conj x)
          (update-in [:future] pop))
    h))

(defn rewind
  "Moves an operation from the past to the future."
  [h]
  (if-let [x (current h)]
    (-> h (update-in [:past] pop)
          (update-in [:future] conj x))
    h))

(defn add
  "Adds an operation to the past of this history removing
  creating a new timeline (removing all operations in the
  current future)."
  [h op]
  (if *save-in-history*
    (-> h
      (update-in [:past] conj op)
      (assoc-in [:future] []))
    h))
  
(defn empty
  "Removes all operations from the history."
  [h]
  (and (core/empty (:past h))
       (core/empty (:future h))))

(defn empty?
  "Returns true if the history has no past or present operations."
  [h]
  (and (core/empty? (:past h))
       (core/empty? (:future h))))
