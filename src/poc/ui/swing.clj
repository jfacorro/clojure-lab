(remove-ns 'poc.ui.swing)
(ns poc.ui.swing
  (:use [poc.ui.protocols :only [Component create set-attr impl]])
  (:require [clojure.string :as str]))

(extend-protocol Component
  java.awt.Container
  (add [this child]
    (.add this child))
  javax.swing.JTabbedPane
  (add [this child]
    (.add this child)))

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
(defn setter [prop n]
  (let [args (take n (repeatedly gensym))]
    (eval `(fn [x# ~@args] (. x# ~(property-accesor :set prop) ~@args)))))

(defmacro getter [prop]
  (eval `(fn [x#] (. x# ~(property-accesor :get prop)))))


(defmethod set-attr :default
  [c k args]
  (let [ctrl  (impl c)
        args  (if (sequential? args) args [args])
        n     (count args)]
    (apply (setter k n) ctrl args)))

;; Window
(defmethod create :window
  [component]
  (javax.swing.JFrame.))

#_(defmethod set-attr [:window :size]
  [c _ [width height]]
  (.setSize (impl c) width height))

(defmethod set-attr [:window :menu]
  [c _ menu]
  (set-attr c :j-menu-bar (impl menu)))

;; Menu Components
(defmethod create :menu-bar [component]
  (javax.swing.JMenuBar.))
  
(defmethod create :menu [component]
  (javax.swing.JMenu.))

(defmethod create :menu-item [component]
  (javax.swing.JMenuItem.))
  
;; Tabbed Component
(defmethod create :tabs [component]
  (javax.swing.JTabbedPane.))

;; Text Editor
(defmethod create :text-editor [component]
  (javax.swing.JTextPane.))
