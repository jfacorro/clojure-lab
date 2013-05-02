(ns lab.ui.swing
  (:import [javax.swing UIManager JFrame JMenuBar JMenu JMenuItem JTabbedPane 
                        JScrollPane JTextPane JTree JSplitPane JButton JPanel
                        JButton JLabel AbstractAction]
           [javax.swing.tree TreeNode DefaultMutableTreeNode DefaultTreeModel]           
           [javax.swing.event TreeSelectionListener]
           [java.awt Font Dimension]
           [java.awt.event MouseAdapter])
  (:use    [lab.ui.protocols :only [Component initialize set-attr impl 
                                    Visible visible? hide show
                                    Selected get-selected set-selected]])
  (:require [lab.util :as util]
            [lab.ui.core :as ui]))
;;------------------- 
(UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName))
;;-------------------
;;-------------------
(extend-protocol Visible
  java.awt.Container
  (visible? [this] (.isVisible this))
  (hide [this] (.setVisible this false))
  (show [this] (.setVisible this true)))

(extend-protocol Component
  java.awt.Container
  (add [this child]
    (.add this child)
    (.validate this)
    this)
  (remove [this child]
    (.remove this child)
    this)
  JTabbedPane
  (add [this child]
    (.addTab this (.getTitle child) child)
    (.setTabComponentAt this (dec (.getTabCount this)) (impl (.getHeader child)))
    (set-selected this (dec (.getTabCount this)))
    this)
  (remove [this child]
    (.remove this child)
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
(extend-protocol Selected
  JTree 
  (get-selected [this]
    (when-let [node (-> this .getLastSelectedPathComponent)]
      (.getUserObject node)))
  JTabbedPane
  (get-selected [this]
    (.getSelectedIndex this))
  (set-selected [this index]
    (.setSelectedIndex this index)))
;;-------------------
(defmacro definitializations
  "Generates all the multimethod implementations
  for each of the entries in the map m."
  [& {:as m}]
  `(do
      (remove-all-methods initialize)
    ~@(for [[k c] m]
      (if (-> c resolve class?)
        `(defmethod initialize ~k [~'_] (new ~c))
        `(defmethod initialize ~k [x#] (~c x#))))))
;;-------------------
(defmacro defattributes
  "Convenience macro to define attribute setters for each
  component type. The method implemented always returns the
  first argument which is supposed to be the component itself."
  [& body]
  (let [comps (->> body (partition-by keyword?) (partition 2) (map #(apply concat %)))
        f     (fn [tag & mthds]
                (for [[attr [c _ _ :as args] & body] mthds]
                  `(defmethod set-attr [~tag ~attr] ~args ~@body ~c)))]
    `(do ~@(mapcat (partial apply f) comps))))
;;-------------------
(defn initialize-font [component]
  (Font. (-> component :attrs :name) 0 (-> component :attrs :size)))

;; Call the macro that generates all initialize multimethod implementations
(definitializations
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
  :panel       JPanel
  ;; Tree
  :tree        JTree
  :tree-node   DefaultMutableTreeNode
  ;; Controls
  :button      JButton
  :label       JLabel)
;;-------------------
;; Setter & Getters
;;-------------------
(defn setter!
  "Generate a setter interop function that takes n arguments."
  [prop n]
  (let [args (take n (repeatedly gensym))]
    (eval `(fn [x# ~@args] (. x# ~(util/property-accesor :set prop) ~@args)))))

(defmacro getter
  "Generate a getter interop function."
  [prop]
  (eval `(fn [x#] (. x# ~(util/property-accesor :get prop)))))
;;-------------------
(defmethod set-attr :default
  [c k args]
  (let [ctrl     (impl c)
        args-seq (if (sequential? args) args [args])
        n        (count args-seq)]
    (apply (setter! k n) ctrl args-seq)
    c))
;;-------------------
(def ^:private split-orientations
  "Split pane possible orientations."
  {:vertical JSplitPane/VERTICAL_SPLIT :horizontal JSplitPane/HORIZONTAL_SPLIT})
;;-------------------
(defattributes
  :window
  (:menu [c k v]
    (.setJMenuBar (impl c) (impl v)))

  :tabs
  (:scroll [c k v]
    (.setJMenuBar (impl c) (impl v)))

  :tree
  (:root [c k v]
    (let [model (DefaultTreeModel. (impl v))]
      (.setModel (impl c) model)))
  (:on-selected [c attr handler]
    (let [listener (proxy [TreeSelectionListener] []
                     (valueChanged [e]
                       (handler (get-selected c))))]
      (.addTreeSelectionListener (impl c) listener)))
  (:on-dbl-click [c attr handler]
    (let [listener (proxy [MouseAdapter] []
                     (mousePressed [e]
                       (when (= 2 (.getClickCount e))
                         (handler (get-selected c)))))]
      (.addMouseListener (impl c) listener)))

  :tree-node 
  (:item [c attr item]
    (.setUserObject (impl c) item))

  :split
  (:orientation [c attr value]
    (.setOrientation (impl c) (split-orientations value)))

  :text-editor 
  (:font [c attr value]
    (.setFont (impl c) (impl value)))
  
  :button 
  (:preferred-size [c attr [w h :as value]]
    (.setPreferredSize (impl c) (Dimension. w h)))
  (:on-click [c _ f]
    (let [action (proxy [AbstractAction] []
                    (actionPerformed [e] (f c)))]
      (.setAction (impl c) action)))

  :font
  (:name [c attr value])
  (:size [c attr value]))
;------------------------------