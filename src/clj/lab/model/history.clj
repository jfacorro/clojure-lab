(ns lab.model.history
  "API to create and manipulate a history of operations."
  (:refer-clojure :exclude [empty empty?])
  (:require [clojure.core :as core]))

(defprotocol Undoable
  (redo [this] "Returns a function that redoes some operation.")
  (undo [this] "Returns a function that undoes some operation."))

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
was disabled upstream, you might like to make 
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
    {:past    past
     :present nil
     :future  fut}))

(defn current
  "Returns the last operation added."
  [{:keys [present] :as h}]
  present)

(defn forward
  "Moves an operation from the future to the past."
  [{:keys [past present future] :as h}]
  (if (or present (peek future))
    (assoc h
           :past    (if present (conj past present) past)
           :present (peek future)
           :future  (or (and (peek future)
                             (pop future))
                        []))
    h))

(defn fast-forward
  "Moves all operations from the future to the past."
  [{:keys [future present past] :as h}]
  (assoc h
         :past    (-> (if present (conj past present) past)
                      (into (reverse future)))
         :present nil
         :future  []))

(defn rewind
  "Moves an operation from the past to the future."
  [{:keys [future present past] :as h}]
  (if (or present (peek past))
    (assoc h
           :past    (or (and (peek past)
                             (pop past))
                        [])
           :present (peek past)
           :future  (if present (conj future present) future))
    h))

(defn add
  "Adds an operation to the past of this history removing
  creating a new timeline (removing all operations in the
  current future)."
  [{:keys [future present past] :as h} op]
  (if *save-in-history*
    (assoc h
      :past    (if present (conj past present) past)
      :present op
      :future  [])
    h))
  
(defn empty
  "Removes all operations from the history."
  [_]
  (history))

(defn empty?
  "Returns true if the history has no past or present operations."
  [{:keys [future present past] :as h}]
  (and (core/empty? past)
       (nil? present)
       (core/empty? future)))
