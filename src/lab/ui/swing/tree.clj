(ns lab.ui.swing.tree
  (:import  [javax.swing JTree]
            [javax.swing.tree TreeNode DefaultMutableTreeNode DefaultTreeModel]
            [javax.swing.event TreeSelectionListener])
  (:use     [lab.ui.protocols :only [Component Selected Implementation impl abstract]])
  (:require [lab.ui.core :as ui]))

(defn tree-node [c]
  (let [ab (atom nil)]
    (proxy [DefaultMutableTreeNode lab.ui.protocols.Implementation] []
      (abstract
        ([] @ab)
        ([x] (reset! ab x) this)))))

(ui/definitializations
  :tree        JTree
  :tree-node   tree-node)

(ui/defattributes
  :tree
    (:on-selected [c _ handler]
      (let [listener (proxy [TreeSelectionListener] []
                       (valueChanged [e]
                         (handler (ui/selected c))))]
        (.addTreeSelectionListener ^JTree (impl c) listener)))
  
  :tree-node
    (:item [c attr item]
      (.setUserObject ^DefaultMutableTreeNode (impl c) item)))

(extend-type DefaultMutableTreeNode
  Component
  (add [this child]
    (.add this child)
    this)
  (remove [this child]
    (.remove this child))
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
  (remove [this child]
    (.remove this child))
  (children [this]
    (.getComponents this))

  Selected
  (selected 
    ([this]
      (when-let [node (-> this .getLastSelectedPathComponent)]
        (ui/attr (abstract node) :id)))
    ([this selected]
      (throw (UnsupportedOperationException. "Set selected item for :tree UI implementation.")))))
