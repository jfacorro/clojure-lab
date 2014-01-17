(ns event)

(defn children [c]
  (-> c .getComponents seq))
  
(defn all-children [c]
  (let [ch (children c)]
    (flatten (concat (children c) (map all-children ch)))))

(defn x-map [c f]
  (loop [im (f c)
         x  {}]
     (let [x (reduce #(assoc-in %1 [%2] (.get im %2)) x (.keys im))
           p (.getParent im)]
       (if p
         (recur p x)
         x))))

(defn action-map [c]
  (x-map c #(.getActionMap %)))

(defn input-map [c]
  (x-map c #(.getInputMap %)))

(defn print-map [f c]
  (doseq [[ks action] (->> c f (sort-by str))]
    (println ks)))

(defn print-seq [s]
  (doseq [x s] (println x)))


(->> @proto.main/main
  :main
  .getRootPane
  .getContentPane
  all-children
  (filter #(instance? javax.swing.JTextPane %))
  first
  .getRegisteredKeyStrokes
  seq
  (sort-by str)
  print-seq
  #_(
  (print-map input-map)
  ;.getInputMethodListeners
  count
  )
)



; JComponent.managingFocusForwardTraversalKeys