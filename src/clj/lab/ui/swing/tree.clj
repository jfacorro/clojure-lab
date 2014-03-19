(ns lab.ui.swing.tree
  (:use     [lab.ui.protocols :only [Component Selection Implementation impl abstract to-map
                                     listen ignore]])
  (:require [lab.ui.core :as ui]
            [lab.ui.util :refer [defattributes definitializations]])
  (:import  [javax.swing JTree JTree$DynamicUtilTreeNode]
            [javax.swing.tree TreeNode DefaultMutableTreeNode DefaultTreeModel TreePath DefaultTreeModel]
            [javax.swing.event TreeSelectionListener TreeExpansionListener 
                               TreeExpansionEvent]
            [java.awt.event MouseAdapter KeyAdapter]))

(defn- update-tree-from-node [^DefaultMutableTreeNode node]
  (let [root (.getRoot node)
        tree ^JTree (:tree (meta root))]
      (when (and tree (.getModel tree))
        (.reload ^DefaultTreeModel (.getModel tree) node))))

(defn tree-node-init [c] 
  (let [ab        (atom nil)
        meta-data (atom nil)
        children  (when-not (ui/attr c :leaf) (to-array []))]
    (proxy [JTree$DynamicUtilTreeNode
            lab.ui.protocols.Implementation
            clojure.lang.IObj]
           [(ui/attr c :item) children]
      (abstract
        ([] @ab)
        ([x] (reset! ab x) this))
      (meta [] @meta-data)
      (withMeta [x]
        (reset! meta-data x)
        this))))

(defn- node-expansion
  "Takes a TreeExpansionEvent, looks for the expanded node
and fires the :expansion handler in it, if there is one.
The handler should return falsey if the node was modified."
  [^TreeExpansionEvent e]
  (let [tree ^JTree (.getSource e)
        node (.. e getPath getLastPathComponent)
        abs  (abstract node)
        fns  (ui/listeners abs :expansion)
        e    (assoc (to-map e) :source abs)]
    (doseq [f fns]
      (when (and f (#'ui/event-handler f e))
        ;; notify the model to reload the modified node
        (update-tree-from-node node)))))

(defn- node-event
  "Event handler for either click or key.
event can be :click or :key."
  [event e]
  (let [tree ^JTree (.getSource ^java.util.EventObject e)
        node (.getLastSelectedPathComponent tree)
        abs  (and node (abstract node))
        fns  (ui/listeners abs event)
        e    (assoc (to-map e) :source abs)]
    (doseq [f fns]
      (when (and node f (#'ui/event-handler f e))
        ;; notify the model to reload the modified node
        (update-tree-from-node node)))))

(defn tree-init [c]
  (let [expansion (proxy [TreeExpansionListener] []
                    (treeCollapsed [e])
                    (treeExpanded [e] (#'node-expansion e)))
        click     (proxy [MouseAdapter] []
                       (mousePressed [e] (#'node-event :click e)))
        key       (proxy [KeyAdapter] []
                    (keyPressed [e] (#'node-event :key e))
                    (keyReleased [e] (#'node-event :key e))
                    (keyTyped [e] (#'node-event :key e)))]
    (doto (JTree.)
      (.setModel (DefaultTreeModel. nil))
      (.addTreeExpansionListener expansion)
      (.addMouseListener click)
      (.addKeyListener key))))

(definitializations
  :tree        tree-init
  :tree-node   tree-node-init)

(extend-type DefaultMutableTreeNode
  Component
  (add [this child]
    (.add this child)
    (update-tree-from-node this)
    this)
  (remove [this child]
    (.remove this ^DefaultMutableTreeNode child)
    (update-tree-from-node this)
    this)
  (children [this]
    (.children this))
  (focus [this] this)

  Implementation
  (abstract
    ([this] (.abstract ^lab.ui.protocols.Implementation this))
    ([this x]
      (.abstract ^lab.ui.protocols.Implementation this x)
      this)))

(extend-type JTree
  Component
  (add [this child]
    (let [model (DefaultTreeModel. (with-meta child {:tree this}))]
      (.setModel this model)
      this))
  (remove [this ^TreeNode child]
    (let [model ^DefaultTreeModel (.getModel this)]
      (if (nil? (.getParent child))
        (.setModel this nil)
        (.removeNodeFromParent model ^DefaultMutableTreeNode child)))
      this)
  (children [this]
    (.getComponents this))
  (focus [this]
    (.requestFocusInWindow this))

  Selection
  (selection
    ([this]
      (when-let [node ^lab.ui.protocols.Implementation (.getLastSelectedPathComponent this)]
        (-> node abstract (ui/attr :id))))
    ([this row]
      (.setSelectionRow this row)
      this)))

(defattributes
  :tree
  (:hide-root [c _ v]
    (.setRootVisible ^JTree (impl c) (not v)))
  :tree-node
    (:leaf [c _ v])
    (:item [c attr item]
      (.setUserObject ^DefaultMutableTreeNode (impl c) item))
    (:info [c _ v]))

;; Since the implementation of these events for the tree nodes
;; actually works through the tree events, there's nothing to do
;; here.
(defmethod listen [:tree-node :key] [c evt f])
(defmethod ignore [:tree-node :key] [c evt f])

(defmethod listen [:tree-node :expansion] [c evt f])
(defmethod ignore [:tree-node :expansion] [c evt f])

(defmethod listen [:tree-node :click] [c evt f])
(defmethod ignore [:tree-node :click] [c evt f])
