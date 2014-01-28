(ns lab.ui.swing.tree
  (:use     [lab.ui.protocols :only [Component Selection Implementation impl abstract to-map]])
  (:require [lab.ui.core :as ui])
  (:import  [javax.swing JTree JTree$DynamicUtilTreeNode]
            [javax.swing.tree TreeNode DefaultMutableTreeNode DefaultTreeModel]
            [javax.swing.event TreeSelectionListener TreeExpansionListener 
                               TreeExpansionEvent]
            [java.awt.event MouseAdapter]))

(defn tree-node-init [c] 
  (let [ab        (atom nil)
        children  (when-not (ui/attr c :leaf) (to-array []))]
    (proxy [JTree$DynamicUtilTreeNode lab.ui.protocols.Implementation]
           [(ui/attr c :item) children]
      (abstract
        ([] @ab)
        ([x] (reset! ab x) this)))))

(defn- on-node-expansion
  "Takes a TreeExpansionEvent, looks for the expanded node
and fires the :on-expansion handler in it, if there is one.
The handler should return falsey if the node was modified."
  [^TreeExpansionEvent e]
  (let [tree ^JTree (.getSource e)
        node (.. e getPath getLastPathComponent)
        abs  (abstract node)
        f    (ui/attr abs :on-expansion)
        e    (assoc (to-map e) :source abs)]
    (when (and f (#'ui/event-handler f e))
      ;; notify the model to reload the modified node
      (.reload ^DefaultTreeModel (.getModel tree) node))))

(defn- on-node-click
  [e]
  (let [tree ^JTree (.getSource e)
        node (.getLastSelectedPathComponent tree)
        abs  (and node (abstract node))
        f    (ui/attr abs :on-click)
        e    (assoc (to-map e) :source abs)]
    (when (and node f (#'ui/event-handler f e))
      ;; notify the model to reload the modified node
      (.reload ^DefaultTreeModel (.getModel tree) node))))

(defn tree-init [c]
  (let [expansion (proxy [TreeExpansionListener] []
                    (treeCollapsed [e])
                    (treeExpanded [e] (#'on-node-expansion e)))
        click     (proxy [MouseAdapter] []
                       (mousePressed [e] (#'on-node-click e)))]
    (doto (JTree.)
      (.setModel (DefaultTreeModel. nil))
      (.addTreeExpansionListener expansion)
      (.addMouseListener click))))

(ui/definitializations
  :tree        tree-init
  :tree-node   tree-node-init)

(extend-type DefaultMutableTreeNode
  Component
  (add [this child]
    (.add this child)
    this)
  (remove [this child]
    (.remove this ^DefaultMutableTreeNode child))
  (children [this]
    (.children this))

  Implementation
  (abstract
    ([this] (.abstract ^lab.ui.protocols.Implementation this))
    ([this x]
      (.abstract ^lab.ui.protocols.Implementation this x)
      this)))

(extend-type JTree
  Component
  (add [this child]
    (let [model (DefaultTreeModel. child)]
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

  Selection
  (selection
    ([this]
      (when-let [node (.getLastSelectedPathComponent this)]
        (abstract node)))
    ([this selection]
      (throw (UnsupportedOperationException. "Set selection item for :tree UI implementation.")))))

(ui/defattributes
  :tree-node
    (:leaf [c _ v])
    (:item [c attr item]
      (.setUserObject ^DefaultMutableTreeNode (impl c) item))
    (:info [c _ v])
    (:on-expansion [c _ v])
    (:on-click [c _ v]))
