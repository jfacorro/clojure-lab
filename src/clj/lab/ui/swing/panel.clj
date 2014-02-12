(ns lab.ui.swing.panel
  (:require [lab.ui.core :as ui]
            [lab.ui.swing.util :as util])
  (:use     [lab.ui.protocols :only [Component impl]])
  (:import [javax.swing JPanel JSplitPane JScrollPane JButton JComponent]
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
      (.setBackground  ^BasicSplitPaneDivider (find-divider (impl c)) (util/color v)))
    (:border [c _ v]
      (let [v       (if (sequential? v) v [v])
            border  (apply util/border v)
            split   ^JSplitPane (impl c)
            divider ^BasicSplitPaneDivider (find-divider split)]
        (.setBorder split border)
        (.setBorder divider border)))
    (:resize-weight [c _ v]
      (.setResizeWeight ^JSplitPane (impl c) v))
    (:divider-location [c _ v]
      (if (integer? v)
        (.setDividerLocation ^JSplitPane (impl c) ^int v)
        (.setDividerLocation ^JSplitPane (impl c) ^float v)))
    (:divider-location-right [c _ v]
      (let [split   ^JSplitPane (impl c)
            orientation (.getOrientation split)
            size    (if (= orientation (util/split-orientations :horizontal))
                       (.getWidth split)
                       (.getHeight split))]
        (if (float? v)
          (.setDividerLocation split (float (- 1 (/ v size))))
          (ui/attr c :divider-location (- size v)))))
    (:divider-size [c _ v]
      (.setDividerSize ^JSplitPane (impl c) v))
    (:orientation [c _ v]
      (.setOrientation ^JSplitPane (impl c) (util/split-orientations v)))
  :scroll
    (:vertical-increment [c _ v]
      (.. ^JScrollPane (impl c) getVerticalScrollBar (setUnitIncrement 16)))
    (:margin-control [c _ v]
      (.setRowHeaderView ^JScrollPane (impl c) (impl v))))

(extend-protocol Component
  JSplitPane
  (add [this child]
    ; Assume that if the top component is a button then 
    ; it is because it was never set
    (if (instance? JButton (.getTopComponent this))
      (.setTopComponent this child)
      (.setBottomComponent this child))
    (util/remove-focus-traversal child)
    this)

  JScrollPane
  (add [this child]
    (.. this getViewport (add ^java.awt.Container child nil))
    (util/remove-focus-traversal child)
    this))
