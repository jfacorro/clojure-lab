(ns lab.ui.swing
  (:use [lab.ui.protocols :only [Component create set-attr impl]]
        lab.ui.core)
  (:require [clojure.string :as str]))

(extend-protocol Component
  java.awt.Container
  (add [this child]
    (.add this child)
    this)
  javax.swing.JTabbedPane
  (add [this child]
    (.addTab this "" child)
    this)
  javax.swing.JScrollPane
  (add [this child]
    (.. this getViewport (add child nil))
    this))

;;-------------------
;; Implementation create map
;;-------------------
(def create-map
 {:window      javax.swing.JFrame
  :menu-bar    javax.swing.JMenuBar
  :menu        javax.swing.JMenu
  :menu-item   javax.swing.JMenuItem
  :tabs        javax.swing.JTabbedPane
  :tab         javax.swing.JScrollPane
  :text-editor javax.swing.JTextPane})

(defmacro create-all-implementations []
  `(do
    ~@(for [[k c] create-map]
      (if (class? c)
        `(defmethod create ~k [~'_] (new ~c))
        `(defmethod create ~k [~'_] (~c))))))

(create-all-implementations)

;;-------------------
;; Setter & Getters
;;-------------------
(defn- capitalize-word [[x & xs]]
  (apply str (str/upper-case x) xs))
;;-------------------
(defn- capitalize [s]
  (->> (str/split s #"-")      
      (map capitalize-word)
      (apply str)))
;;-------------------
(defn- property-accesor [op prop]
  (symbol (str (name op) (-> prop name capitalize))))
;;-------------------
(defn setter
  "Generate a setter interop function that takes n arguments."
  [prop n]
  (let [args (take n (repeatedly gensym))]
    (eval `(fn [x# ~@args] (. x# ~(property-accesor :set prop) ~@args)))))

(defmacro getter
  "Generate a getter interop function."
  [prop]
  (eval `(fn [x#] (. x# ~(property-accesor :get prop)))))

(defmethod set-attr :default
  [c k args]
  (let [ctrl     (impl c)
        args-seq (if (sequential? args) args [args])
        n        (count args-seq)]
    (apply (setter k n) ctrl args-seq)
    (assoc-in c [:attrs k] args)))

;;-------------------
;; window attributes
;;-------------------
(defmethod set-attr [:window :menu]
  [c _ menu]
  (.setJMenuBar (impl c) (impl menu))
  (assoc-in c [:attrs :menu] menu))
