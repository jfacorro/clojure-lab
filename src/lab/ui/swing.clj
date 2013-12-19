(ns lab.ui.swing
  (:refer-clojure :exclude [remove])
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
                          misc-control
                          event])
  (:import [javax.swing UIManager JComponent AbstractAction SwingUtilities]
           [java.awt Dimension]
           [java.awt.event MouseAdapter FocusAdapter]))

(defmacro swing-action
  "Queues an action to the event queue."
  [& body]
  `(SwingUtilities/invokeLater 
    (fn [] ~@body)))

(alter-var-root #'lab.ui.core/ui-action-macro #(do % %2) 'lab.ui.swing/swing-action)
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

;; Definition of attribute setters for each kind
;; of component in the hierarchy.
(ui/defattributes
  :component
    (:id [c _ v]
      (when (not= (ui/attr c :id) v)
        (throw (Exception. (str "Can't change the :id once it is set: " c)))))
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
      (.setVisible ^java.awt.Component (p/impl c) v))
    ;;;;;;;;;;;
    ;; Events
    (:key-event [c _ _])
    (:on-focus [c _ handler]
      (let [listener (proxy [FocusAdapter] []
                       (focusGained [e] (handler (p/to-map e))))]
        (.addFocusListener ^JComponent (p/impl c) listener)))
    (:on-blur [c _ handler]
      (let [listener (proxy [FocusAdapter] []
                       (focusLost [e] (handler (p/to-map e))))]
        (.addFocusListener ^JComponent (p/impl c) listener)))
    (:on-click [c _ handler]
      (let [listener (proxy [MouseAdapter] []
                       (mousePressed [e] (handler (p/to-map e))))]
        (.addMouseListener ^JComponent (p/impl c) listener))))
