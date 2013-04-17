(ns lab.util)

(defmacro !
  "Applies f to the atom x using the supplied arguments.
  Convenience macro."
  [f x & args]
  `(swap! ~x ~f ~@args))