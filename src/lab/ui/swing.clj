(ns lab.ui.swing
  (:import [javax.swing UIManager JFrame JMenuBar JMenu JMenuItem JTabbedPane 
                        JScrollPane JTextPane JTree JSplitPane JButton]
           [javax.swing.tree DefaultMutableTreeNode DefaultTreeModel])
  (:use    [lab.ui.protocols :only [Component create set-attr impl]]
           lab.ui.core)
  (:require [clojure.string :as str]))
;;------------------- 
(UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName))
;;-------------------
(extend-protocol Component
  java.awt.Container
  (add [this child]
    (.add this child)
    this)
  JTabbedPane
  (add [this child]
    (.addTab this "" child)
    this)
  JSplitPane
  (add [this child]
    (println "split ->" (.getTopComponent this))
    (if (instance? JButton (.getTopComponent this))
      (.setTopComponent this child)
      (.setBottomComponent this child))
    this)
  JScrollPane
  (add [this child]
    (.. this getViewport (add child nil))
    this)
  DefaultMutableTreeNode
  (add [this child]
    (.add this child)
    this))
;;-------------------
;; create - multimethod
;;-------------------
(defmacro defmethods-create
  "Generates all the multimethod implementations
  for each of the entries in the map m."
  [& {:as m}]
  `(do
    ~@(for [[k c] m]
      (if (-> c resolve class?)
        `(defmethod create ~k [~'_] (new ~c))
        `(defmethod create ~k [x#] (~c x#))))))

(defn create-tree-node
  "Creates a TreeNode proxy for the component"
  [component]
  (proxy [TreeNode] []
    (children []
      (->> component :content))
    (getAllowsChildren [] true)
    (getChildAt [i]
      (-> component :content (nth i)))
    (getChildCount [] 
      (-> component :content count))
    (getIndex [node] nil)
    (getParent [] nil)
    (isLeaf []
      (-> component :content count zero?))))

;; Call the macro that generates all create multimethod implementations
(defmethods-create
  :window      JFrame
  :menu-bar    JMenuBar
  :menu        JMenu
  :menu-item   JMenuItem
  :tabs        JTabbedPane
  :tab         JScrollPane
  :text-editor JTextPane
  :tree        JTree
  :tree-node   DefaultMutableTreeNode
  :split       JSplitPane)
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
(defn setter!
  "Generate a setter interop function that takes n arguments."
  [prop n]
  (let [args (take n (repeatedly gensym))]
    (eval `(fn [x# ~@args] (. x# ~(property-accesor :set prop) ~@args)))))

(defmacro getter
  "Generate a getter interop function."
  [prop]
  (eval `(fn [x#] (. x# ~(property-accesor :get prop)))))
;;-------------------
(defmethod set-attr :default
  [c k args]
  (let [ctrl     (impl c)
        args-seq (if (sequential? args) args [args])
        n        (count args-seq)]
    (apply (setter! k n) ctrl args-seq)
    (assoc-in c [:attrs k] args)))
;;-------------------
;; window attributes
;;-------------------
(defmethod set-attr [:window :menu]
  [c _ menu]
  (.setJMenuBar (impl c) (impl menu))
  (assoc-in c [:attrs :menu] menu))


(defmethod set-attr [:tree :root]
  [c _ root]
  (let [model (DefaultTreeModel. (impl root))]
    (.setModel (impl c) model)
    (assoc-in c [:attrs :root] root)))

(defmethod set-attr [:tree-node :text]
  [c _ text]
  (.setUserObject (impl c) text)
  (assoc-in c [:attrs :text] text))
