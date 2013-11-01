(ns lab.ui.swing
  (:refer-clojure :exclude [remove])
  (:import [javax.swing UIManager JComponent AbstractAction]
           [java.awt Dimension]
           [java.awt.event MouseAdapter])
  (:require [lab.ui.protocols :as p]
            [lab.ui.core :as ui]
            [lab.ui.swing [util :as util]
                          window
                          panel
                          dialog
                          tree
                          menu
                          text
                          tab
                          misc-control]))
;;------------------- 
(UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName))
;;-------------------
  
(extend-protocol p/Implementation
  JComponent
  (abstract 
    ([this]
      (.getClientProperty this :abstract))
    ([this the-abstract] 
      (.putClientProperty this :abstract the-abstract)
      this)))

(extend-protocol p/Component
  java.awt.Container
  (add [this child]
    (.add this ^java.awt.Component child)
    (.validate this)
    this)
  (remove [this child]
    (.remove this ^java.awt.Component child)
    this)
    
  JComponent
  (add [this child]
    (.add this ^JComponent child)
    (.validate this)
    this)
  (remove [this ^JComponent child]
    (.remove this child)
    this)
  (add-binding [this ks f]
    (let [im   (.getInputMap this)
          am   (.getActionMap this)
          ks   (util/keystroke ks)
          desc (or (-> f meta :name) (name (gensym "binding-")))
          act  (proxy [AbstractAction] []
                 (actionPerformed [e] (f e)))]
      (.put im ks desc)
      (.put am desc act)
      this))
  (remove-binding [this ks]
    (let [im   (.getInputMap this)
          am   (.getActionMap this)
          ks   (util/keystroke ks)
          desc (.get im ks)]
      (when desc
        (.remove im ks)
        (.remove am desc))
      this)))

(extend-protocol p/Event
  java.util.EventObject
  (to-map [this]
    {:source (.getSource this)})
  java.awt.event.MouseEvent
  (to-map [this]
    {:source       (.getSource this)
     :button       (.getButton this)
     :click-count  (.getClickCount this)
     :screen-loc   (as-> (.getLocationOnScreen this) p [(.getX p) (.getY p)])
     :point        (as-> (.getPoint this) p [(.getX p) (.getY p)])}))

;; Definition of attribute setters for each kind
;; of component in the hierarchy.
(ui/defattributes
  :component
    (:id [c _ _] c)
    (:transparent [c _ v]
      (.setOpaque ^JComponent (p/impl c) (not v)))
    (:border [c _ v]
      (let [v (if (sequential? v) v [v])]
        (.setBorder ^JComponent (p/impl c) (apply util/border v))))
    (:background [c _ v]
      (.setBackground ^JComponent (p/impl c) (util/color v)))
    (:foreground [c _ v]
      (.setForeground ^JComponent (p/impl c) (util/color v)))
    (:font [c _ v]
      (.setFont ^JComponent (p/impl c) (util/font v)))
    (:size [c attr [w h :as v]]
      (.setPreferredSize ^JComponent (p/impl c) (Dimension. w h)))
    (:visible [c _ v]
      (.setVisible (p/impl c) v))
    ; events
    (:on-click [c _ handler]
      (let [listener (proxy [MouseAdapter] []
                       (mousePressed [e]
                         (-> e p/to-map handler)))]
        (.addMouseListener ^JComponent (p/impl c) listener))))
