(ns lab.ui.swing.tree
  (:import  [javax.swing JTree JTree$DynamicUtilTreeNode]
            [javax.swing.tree TreeNode DefaultMutableTreeNode DefaultTreeModel]
            [javax.swing.event TreeSelectionListener
                               TreeExpansionListener TreeExpansionEvent])
  (:use     [lab.ui.protocols :only [Component Selected Implementation impl abstract]])
  (:require [lab.ui.core :as ui]))

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
        ab   (abstract node)
        f    (ui/attr ab :on-expansion)]
    (when (and f (f e))
      ;; notify the model to reload the modified node
      (.reload ^DefaultTreeModel (.getModel tree) node))))

(defn tree-init [c]
  (let [l (proxy [TreeExpansionListener] []
            (treeCollapsed [e])
            (treeExpanded [e] (#'on-node-expansion e)))]
    (doto (JTree.)
      (.setModel (DefaultTreeModel. nil))
      (.addTreeExpansionListener l))))

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

  Selected
  (selected 
    ([this]
      (when-let [node (.getLastSelectedPathComponent this)]
        (ui/attr (abstract node) :id)))
    ([this selected]
      (throw (UnsupportedOperationException. "Set selected item for :tree UI implementation.")))))

(ui/defattributes
  :tree
    (:on-expansion [c _ f]
      (let [listener (proxy [TreeExpansionListener] []
                       (treeCollapsed [e])
                       (treeExpanded [e] (f e)))]
        (.addTreeExpansionListener ^JTree (impl c) listener)))
    (:on-selected [c _ f]
      (let [listener (proxy [TreeSelectionListener] []
                       (valueChanged [e] (f (ui/selected c))))]
        (.addTreeSelectionListener ^JTree (impl c) listener)))
  
  :tree-node
    (:leaf [c _ v])
    (:on-expansion [c _ v])
    (:item [c attr item]
      (.setUserObject ^DefaultMutableTreeNode (impl c) item)))
