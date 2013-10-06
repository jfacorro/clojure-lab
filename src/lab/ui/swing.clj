(remove-ns 'lab.ui.swing)
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
                          file-dialog
                          tree
                          menu
                          text
                          tab
                          misc-control]))
;;------------------- 
(UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName))
;;-------------------
(extend-protocol p/Visible
  java.awt.Container
  (visible? [this] (.isVisible this))
  (hide [this] (.setVisible this false))
  (show [this] (.setVisible this true)))
  
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
    (.add this child)
    (.validate this)
    this)
  (remove [this child]
    (.remove this child)
    this)
    
  JComponent
  (add [this child]
    (.add this child)
    (.validate this)
    this)
  (remove [this child]
    (.remove this child)
    this)
  (add-binding [this ks f]
    (let [im   (.getInputMap this)
          am   (.getActionMap this)
          ks   (util/key-stroke ks)
          desc (or (-> f meta :name) (name (gensym "binding-")))
          act  (proxy [AbstractAction] []
                 (actionPerformed [e] (f e)))]
      (.put im ks desc)
      (.put am desc act)
      this))
  (remove-binding [this ks]
    (let [im   (.getInputMap this)
          am   (.getActionMap this)
          ks   (util/key-stroke ks)
          desc (.get im ks)]
      (when desc
        (.remove im ks)
        (.remove am desc))
      this)))
;;-------------------
(extend-protocol p/Event
  java.util.EventObject
  (source [this]
    (.getSource this)))

;; Attributes

(defmethod p/set-attr :default
  [c k args]
  "Fall-back method implementation for properties not specified 
  for the component in defattributes.
  Tries to set the property by building a function setter
  and calling it with the supplied args."
  (let [ctrl  (p/impl c)
        args  (if (sequential? args) args [args])
        args  (map #(if (ui/component? %) (p/impl %) %) args)
        n     (count args)]
    (apply (util/setter! (class ctrl) k n) ctrl args)
    c))

;; Definition of attribute setters for each kind
;; of component in the hierarchy.
(ui/defattributes
  :component
    (:border [c _ v]
      (let [v (if (sequential? v) v [v])]
        (.setBorder (p/impl c) (apply util/border v))))
    (:background [c _ v]
      (.setBackground (p/impl c) (util/color v)))
    (:foreground [c _ v]
      (.setForeground (p/impl c) (util/color v)))
    (:font [c _ value]
      (.setFont (p/impl c) (util/font value)))
    (:size [c attr [w h :as value]]
      (.setPreferredSize (p/impl c) (Dimension. w h)))

    (:on-click [c _ handler]
      (let [listener (proxy [MouseAdapter] []
                       (mousePressed [e]
                         (when (= 1 (.getClickCount e)) (handler e))))]
        (.addMouseListener (p/impl c) listener)))
    (:on-dbl-click [c _ handler]
      (let [listener (proxy [MouseAdapter] []
                       (mousePressed [e]
                         (when (= 2 (.getClickCount e)) (handler e))))]
        (.addMouseListener (p/impl c) listener))))
