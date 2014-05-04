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
                (async/thread (if (sequential? args) (apply f args) (f args)))
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

(defn find-char
  "Finds the next char in s for which pred is true,
  starting to look from position i, stepping through the 
  positions determined by the application of the step function.
  Returns the index if found or nil otherwise."
  [s i pred step]
  (cond (or (neg? i) (<= (count s) i)) nil
        (pred (get s i)) i
        :else (recur s (step i) pred step)))

(defn remove-at
  "Removes the element in the ith position from the given vector."
  [v i]
  (vec (concat (subvec v 0 i) (subvec v (inc i) (count v)))))

(defn index-of
  "Takes a vector and returns the index of x."
  [^clojure.lang.PersistentVector v x]
  (.indexOf v x))
