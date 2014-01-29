(ns lab.ui.swing
  (:require [lab.ui.core :as ui])
  (:import [javax.swing SwingUtilities]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; First define macro to perform ui actions
;; and alter the root binding of lab.ui.core/ui-action-macro.

(defmacro swing-action
  "Queues an action to the event queue."
  [& body]
  `(SwingUtilities/invokeLater
    (fn [] ~@body)))

(ui/register-action-macro! #'lab.ui.swing/swing-action)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Then require components implementations

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
           [java.awt.event MouseAdapter FocusAdapter KeyListener]))

;;;;;;;;;;;;;;;;;;;;;;
;; Swing L&F

(UIManager/setLookAndFeel "javax.swing.plaf.metal.MetalLookAndFeel")

;;;;;;;;;;;;;;;;;;;;;;
;; UI protocols implementation
  
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
  (children [this]
    (.getComponents this))
  (add [this child]
    (.add this ^java.awt.Component child)
    (.validate this)
    this)
  (remove [this child]
    (.remove this ^java.awt.Component child)
    this)
  (focus [this]
    (.requestFocus this))
    
  JComponent
  (children [this]
    (.getComponents this))
  (add [this child]
    (.add this ^JComponent child)
    (.validate this)
    this)
  (remove [this ^JComponent child]
    (.remove this child)
    this)
  (focus [this]
    (.grabFocus this)))

;;;;;;;;;;;;;;;;;;;;;;
;; Common attributes for all components

(ui/defattributes
  :component
    (:transparent [c _ v]
      (.setOpaque ^JComponent (p/impl c) (not v)))
    (:layout [c _ v]
      (let [v (if (sequential? v) v [v])
            c (p/impl c)]
        (.setLayout c (util/layout c v))))
    (:border [c _ v]
      (let [v (if (sequential? v) v [v])]
        (.setBorder ^JComponent (p/impl c) (apply util/border v))))
    (:background [c _ v]
      (.setBackground ^JComponent (p/impl c) (util/color v)))
    (:color [c _ v]
      (.setForeground ^JComponent (p/impl c) (util/color v)))
    (:font [c _ v]
      (.setFont ^JComponent (p/impl c) (util/font v)))
    (:size [c attr [w h :as v]]
      (.setPreferredSize ^JComponent (p/impl c) (util/dimension w h)))
    (:visible [c _ v]
      (.setVisible ^java.awt.Component (p/impl c) v))

    ;; Events
    (:on-key [c _ f]
      (let [listener (proxy [KeyListener] []
                       (keyPressed [e] (ui/handle-event f e))
                       (keyReleased [e] (ui/handle-event f e))
                       (keyTyped [e] (ui/handle-event f e)))]
        (.addKeyListener ^JComponent (p/impl c) listener)))
    (:on-focus [c _ f]
      (let [listener (proxy [FocusAdapter] []
                       (focusGained [e] (ui/handle-event f e)))]
        (.addFocusListener ^JComponent (p/impl c) listener)))
    (:on-blur [c _ f]
      (let [listener (proxy [FocusAdapter] []
                       (focusLost [e] (ui/handle-event f e)))]
        (.addFocusListener ^JComponent (p/impl c) listener)))
    (:on-click [c _ f]
      (let [listener (proxy [MouseAdapter] []
                       (mousePressed [e] (ui/handle-event f e)))]
        (.addMouseListener ^JComponent (p/impl c) listener))))
