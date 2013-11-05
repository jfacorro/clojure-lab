(ns lab.ui.swing
  (:refer-clojure :exclude [remove])
  (:import [javax.swing UIManager JComponent AbstractAction]
           [java.awt Dimension]
           [java.awt.event MouseAdapter MouseEvent
                           FocusAdapter FocusEvent
                           ActionEvent])
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

(defn- flag-modifiers [m n]
  (->> m 
    (filter #(pos? (bit-and n (second %))))
    (mapv first)))

(def action-modifiers
  {:alt   ActionEvent/ALT_MASK
   :ctrl  ActionEvent/CTRL_MASK
   :shift ActionEvent/SHIFT_MASK})

(def mouse-button
  {MouseEvent/BUTTON1  :button-1
   MouseEvent/BUTTON2  :button-2
   MouseEvent/BUTTON3  :button-3})

(extend-protocol p/Event
  java.util.EventObject
  (to-map [this]
    {:source (.getSource this)})
  MouseEvent
  (to-map [this]
    {:source       (.getSource this)
     :button       (mouse-button (.getButton this))
     :click-count  (.getClickCount this)
     :screen-loc   (as-> (.getLocationOnScreen this) p [(.getX p) (.getY p)])
     :point        (as-> (.getPoint this) p [(.getX p) (.getY p)])})
  FocusEvent
  (to-map [this]
    {:source    (.getSource this)
     :previous  (.getOppositeComponent this)
     :temporary (.isTemporary this)})
  ActionEvent
  (to-map [this]
    {:source    (.getSource this)
     :modifiers (flag-modifiers action-modifiers (.getModifiers this))}))

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
    (:on-focus [c _ handler]
      (let [listener (proxy [FocusAdapter] []
                       (focusGained [e] (handler (p/to-map e))))]
        (.addFocusListener (p/impl c) listener)))
    (:on-blur [c _ handler]
      (let [listener (proxy [FocusAdapter] []
                       (focusLost [e] (handler (p/to-map e))))]
        (.addFocusListener (p/impl c) listener)))
    (:on-click [c _ handler]
      (let [listener (proxy [MouseAdapter] []
                       (mousePressed [e] (handler (p/to-map e))))]
        (.addMouseListener ^JComponent (p/impl c) listener))))
