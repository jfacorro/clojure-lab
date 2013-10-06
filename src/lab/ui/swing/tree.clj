(ns lab.ui.swing.tree
  (:import  [javax.swing JTree]
            [javax.swing.tree TreeNode DefaultMutableTreeNode DefaultTreeModel]
            [javax.swing.event TreeSelectionListener])
  (:use     [lab.ui.protocols :only [Component Selected get-selected Implementation impl]])
  (:require [lab.ui.core :as ui]))

(ui/definitializations
  :tree        JTree
  :tree-node   DefaultMutableTreeNode)

(ui/defattributes
  :tree
    (:root [c _ v]
      (let [model (DefaultTreeModel. (impl v))]
        (.setModel (impl c) model)))
    (:on-selected [c _ handler]
      (let [listener (proxy [TreeSelectionListener] []
                       (valueChanged [e]
                         (handler (get-selected c))))]
        (.addTreeSelectionListener (impl c) listener)))
  
  :tree-node
    (:item [c attr item]
      (.setUserObject (impl c) item)))

(extend-type DefaultMutableTreeNode
  Component
  (add [this child]
    (.add this child)
    this)
  Implementation
  (abstract
    ([this] nil)
    ([this the-abstract] this)))

(extend-protocol Selected
  JTree 
  (get-selected [this]
    (when-let [node (-> this .getLastSelectedPathComponent)]
      (.getUserObject node))))
