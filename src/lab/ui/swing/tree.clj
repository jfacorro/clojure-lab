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
    (:root [c _ v]
      (let [model (DefaultTreeModel. (impl v))]
        (.setModel (impl c) model)))
    (:on-selected [c _ handler]
      (let [listener (proxy [TreeSelectionListener] []
                       (valueChanged [e]
                         (handler (ui/selected c))))]
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
    ([this] (.abstract this))
    ([this x]
      (.abstract this x)
      this)))

(extend-protocol Selected
  JTree 
  (selected 
    ([this]
      (when-let [node (-> this .getLastSelectedPathComponent)]
        (ui/attr (abstract node) :id)))
    ([this selected]
      (throw (UnsupportedOperationException. "Set selected item for :tree UI implementation.")))))
