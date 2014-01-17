(ns lab.ui.swing.panel
  (:require [lab.ui.core :as ui]
            [lab.ui.swing.util :as util])
  (:use     [lab.ui.protocols :only [Component impl]])
  (:import [javax.swing JPanel JSplitPane JScrollPane JButton]
           [java.awt BorderLayout]
           [javax.swing.plaf.basic BasicSplitPaneDivider]))

(ui/definitializations
  :split       JSplitPane
  :panel       JPanel
  :scroll      JScrollPane)

(defn- find-divider [^JSplitPane split]
  (->> split 
    .getComponents 
    (filter (partial instance? BasicSplitPaneDivider))
    first))

(ui/defattributes
  :split
    (:divider-background [c _ v]
      (.setBackground (find-divider (impl c)) (util/color v)))
    (:border [c _ v]
      (let [v       (if (sequential? v) v [v])
            border  (apply util/border v)
            split   ^JSplitPane (impl c)
            divider (find-divider split)]
        (.setBorder split border)
        (.setBorder divider border)))
    (:resize-weight [c _ v]
      (.setResizeWeight ^JSplitPane (impl c) v))
    (:divider-location [c _ v]
      (.setDividerLocation ^JSplitPane (impl c) v))
    (:divider-size [c _ v]
      (.setDividerSize ^JSplitPane (impl c) v))
    (:orientation [c attr value]
      (.setOrientation ^JSplitPane (impl c) (util/split-orientations value)))
  :scroll
    (:vertical-increment [c _ v]
      (.. (impl c) (getVerticalScrollBar) (setUnitIncrement 16)))
    (:margin-control [c _ v]
      (.setRowHeaderView (impl c) (impl v))))

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
