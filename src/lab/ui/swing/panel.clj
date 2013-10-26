(ns lab.ui.swing.panel
  (:import [javax.swing JPanel JSplitPane JScrollPane JButton])
  (:use     [lab.ui.protocols :only [Component impl]])
  (:require [lab.ui.core :as ui]
            [lab.ui.swing.util :as util]))

(ui/definitializations
  :split       JSplitPane
  :panel       JPanel
  :scroll      JScrollPane)

(ui/defattributes
  :split
    (:resize-weight [c _ v]
      (.setResizeWeight (impl c) v)
      c)
    (:divider-location [c _ value]
      (.setDividerLocation (impl c) value))
    (:orientation [c attr value]
      (.setOrientation (impl c) (util/split-orientations value))))

(extend-protocol Component
  JSplitPane
  (add [this child]
    ; Assume that if the top component is a button then 
    ; it is because it was never set
    (if (instance? JButton (.getTopComponent this))
      (.setTopComponent this child)
      (.setBottomComponent this child))
    this)

  JScrollPane
  (add [this child]
    (.. this getViewport (add child nil))
    this))
