(ns lab.model.history
  "API to create and manipulate a history of operations."
  (:refer-clojure :exclude [empty empty?])
  (:require [clojure.core :as core]))

(defprotocol Bijective
  (inverse [this] "Returns an inverse monadic function of this operation."))

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
  "Returns the last operation in the history."
  [h]
  (peek (:past h)))

(defn forward
  "Moves an operation from future to past."
  [h]
  (let [x (-> h :future peek)]
    (-> h (update-in [:past] conj x)
          (update-in [:future] pop))))

(defn rewind
  "Moves an operation from past to future."
  [h]
  (let [x (current h)]
    (-> h (update-in [:past] pop)
          (update-in [:future] conj x))))

(defn add
  "Adds an operation to the past of this history removing
  creating a new timeline (removing all operations in the
  current future)."
  [h op]
  (-> h (update-in [:past] conj op)
        (assoc-in [:future] [])))

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
