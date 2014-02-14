(ns lab.ui.util)

(defn int-to-rgb
  "Converts a single int value int a RGB triple."
  [n]
  (let [r (-> n (bit-and 0xFF0000) (bit-shift-right 16))
        g (-> n (bit-and 0x00FF00) (bit-shift-right 8))
        b (-> n (bit-and 0x0000FF))]
    {:r r :g g :b b}))

(defn rgb-to-int [{:keys [r g b]}]
  "Converts a RGB triple to a single int value."
  (int (+ (* r 65536) (* g 256) b)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Convenience macros for multimethod implementations

(defmacro definitializations
  "Generates all the multimethod implementations
for each of the entries in the map destructured
from its args.
  
  :component-tag ClassName | init-fn"
  [& {:as m}]
  `(do
    ~@(for [[k c] m]
      (if (and (not (seq? c)) (-> c resolve class?))
        `(defmethod lab.ui.protocols/initialize ~k [c#]
          (new ~c))
        `(defmethod lab.ui.protocols/initialize ~k [x#]
          (~c x#))))))

(defmacro defattributes
  "Convenience macro to define attribute setters for each component type. 

The method implemented returns the first argument (which is the component 
itself), UNLESS the `^:modify` metadata flag is true for the argument vector, 
in which case the value from the last expression in the body is returned.

  *attrs-declaration
  
Where each attrs-declaration is:

  component-tag *attr-declaration
    
And each attr-declaration is:

  (attr-name [c attr v] & body)"
  [& body]
  (let [comps (->> body
                (partition-by keyword?)
                (partition 2)
                (map #(apply concat %)))
        f     (fn [tag & mthds]
                (for [[attr [c _ _ :as args] & body] mthds]
                  (let [x (gensym)]
                    (assert (not= c '_) "First arg symbol can't be _")
                    `(defmethod lab.ui.protocols/set-attr [~tag ~attr]
                      ~args
                      (let [~x (do ~@body)]
                        ~(if (-> args meta :modify) x c))))))]
    `(do ~@(mapcat (partial apply f) comps))))
