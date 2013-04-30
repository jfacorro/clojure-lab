(ns lab.ui.swing
  (:import [javax.swing UIManager JFrame JMenuBar JMenu JMenuItem JTabbedPane 
                        JScrollPane JTextPane JTree JSplitPane JButton]
           [javax.swing.tree TreeNode DefaultMutableTreeNode DefaultTreeModel]           
           [javax.swing.event TreeSelectionListener]
           [java.awt Font]
           [java.awt.event MouseAdapter])
  (:use    [lab.ui.protocols :only [Component initialize set-attr impl 
                                    Selected get-selected]]
           lab.ui.core)
  (:require [clojure.string :as str]))
;;------------------- 
(UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName))
;;-------------------
(extend-protocol Component
  java.awt.Container
  (add [this child]
    (.add this child)
    (.validate this)
    this)
  JTabbedPane
  (add [this child]
    (.addTab this (.getTitle child) child)
    this)
  JSplitPane
  (add [this child]
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
;; initialize - multimethod
;;-------------------
(defmacro defmethods-initialize
  "Generates all the multimethod implementations
  for each of the entries in the map m."
  [& {:as m}]
  `(do
    ~@(for [[k c] m]
      (if (-> c resolve class?)
        `(defmethod initialize ~k [~'_] (new ~c))
        `(defmethod initialize ~k [x#] (~c x#))))))

(defn initialize-font [component]
  (Font. "Consolas" 14 0))

;; Call the macro that generates all initialize multimethod implementations
(defmethods-initialize
  ;; Frame
  :window      JFrame
  ;; Menu
  :menu-bar    JMenuBar
  :menu        JMenu
  :menu-item   JMenuItem
  ;; Panels
  :tabs        JTabbedPane
  :tab         lab.ui.swing.Tab
  ;; Text
  :text-editor JTextPane
  :font        initialize-font
  ;; Layout
  :split       JSplitPane
  ;; Tree
  :tree        JTree
  :tree-node   DefaultMutableTreeNode)
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
  [c attr menu]
  (.setJMenuBar (impl c) (impl menu))
  (assoc-in c [:attrs attr] menu))
;;-------------------
;; tabs attributes
;;-------------------
(defmethod set-attr [:tabs :scroll]
  [c attr value]
  (.setTabLayoutPolicy (impl c)
                       (or (and value JTabbedPane/SCROLL_TAB_LAYOUT)
                           JTabbedPane/WRAP_TAB_LAYOUT))
  (assoc-in c [:attrs attr] value))
;;-------------------
;; tree attributes
;;-------------------
(extend-type JTree
  Selected
  (get-selected [this]
    (when-let [node (-> this .getLastSelectedPathComponent)]
      (.getUserObject node))))

(defmethod set-attr [:tree :root]
  [c attr root]
  (let [model (DefaultTreeModel. (impl root))]
    (.setModel (impl c) model)
    (assoc-in c [:attrs attr] root)))
    
(defmethod set-attr [:tree :on-selected]
  [c attr handler]
  (let [listener (proxy [TreeSelectionListener] []
                   (valueChanged [e]
                     (handler (get-selected c))))]
    (.addTreeSelectionListener (impl c) listener)
    (assoc-in c [:attrs attr] handler)))

(defmethod set-attr [:tree :on-dbl-click]
  [c attr handler]
  (let [listener (proxy [MouseAdapter] []
                   (mousePressed [e]
                     (when (= 2 (.getClickCount e))
                       (handler (get-selected c)))))]
    (.addMouseListener (impl c) listener)
    (assoc-in c [:attrs attr] handler)))

(defmethod set-attr [:tree-node :item]
  [c attr item]
  (.setUserObject (impl c) item)
  (assoc-in c [:attrs attr] item))
;;-------------------
;; split attributes
;;-------------------
(def ^:private split-orientations
  "Split pane possible orientations."
  {:vertical JSplitPane/VERTICAL_SPLIT :horizontal JSplitPane/HORIZONTAL_SPLIT})

(defmethod set-attr [:split :orientation]
  [c _ orientation]
  (.setOrientation (impl c) (split-orientations orientation))
  (assoc-in c [:attrs :orientation] orientation))
;;-------------------
;; font attributes
;;-------------------
(defmethod set-attr [:font :name]
  [c attr item]
  c)
(defmethod set-attr [:font :size]
  [c attr item]
  c)