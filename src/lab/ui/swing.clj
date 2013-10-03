(ns lab.ui.swing
  (:refer-clojure :exclude [remove])
  (:import [javax.swing UIManager JFrame JTabbedPane JScrollPane  
                        JSplitPane JPanel JButton JLabel AbstractAction JComponent]
           [java.awt Dimension]
           [java.awt.event MouseAdapter ActionListener])
  (:use    [lab.ui.protocols :only [Component add remove
                                    initialize set-attr
                                    Abstract impl 
                                    Visible visible? hide show
                                    Implementation abstract
                                    Event source
                                    Selected get-selected set-selected]])
  (:require [lab.ui.core :as ui]
            [lab.ui.swing [util :as swutil]
                          file-dialog
                          tree
                          menu
                          text]))
;;------------------- 
(UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName))
;;-------------------
;;-------------------
(extend-protocol Visible
  java.awt.Container
  (visible? [this] (.isVisible this))
  (hide [this] (.setVisible this false))
  (show [this] (.setVisible this true)))
  
(extend-protocol Implementation
  JComponent
  (abstract 
    ([this]
      (.getClientProperty this :abstract))
    ([this the-abstract] 
      (.putClientProperty this :abstract the-abstract)
      this))

  Object
  (abstract 
    ([this] this)
    ([this the-abstract] this)))

(extend-protocol Component
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
          ks   (swutil/key-stroke ks)
          desc (or (-> f meta :name) (name (gensym "binding-")))
          act  (proxy [AbstractAction] []
                 (actionPerformed [e] (f e)))]
      (.put im ks desc)
      (.put am desc act)
      this))
  (remove-binding [this ks]
    (let [im   (.getInputMap this)
          am   (.getActionMap this)
          ks   (swutil/key-stroke ks)
          desc (.get im ks)]
      (when desc
        (.remove im ks)
        (.remove am desc))
      this))

  JTabbedPane
  (add [this child]
    (let [i         (.getTabCount this)
          child-abs (abstract child)
          header    (when-let [h (ui/get-attr child-abs :-header)] (impl h))
          tool-tip  (ui/get-attr child-abs :-tool-tip)
          title     (ui/get-attr child-abs :-title)]
      (.addTab this title child)
      (when header (.setTabComponentAt this i header))
      (when tool-tip (.setToolTipTextAt this i tool-tip))
      (set-selected this i))
    this)
  (remove [this child]
    (.remove this child)
    this)

  JSplitPane
  (add [this child]
    (if (instance? JButton (.getTopComponent this))
      (.setTopComponent this child)
      (.setBottomComponent this child))
    this)

  JScrollPane
  (add [this child]
    (.. this getViewport (add child nil))
    this))
;;-------------------
(extend-protocol Selected
  JTabbedPane
  (get-selected [this]
    (.getSelectedIndex this))
  (set-selected [this index]
    (.setSelectedIndex this index)))

(extend-protocol Event
  java.util.EventObject
  (source [this]
    (.getSource this)))

;; Initialize multimethod implementations
(swutil/definitializations
  ;; Frame
  :window      JFrame
  ;; Panels
  :tabs        JTabbedPane
  :tab         JScrollPane
  ;; Layout
  :split       JSplitPane
  :panel       JPanel
  ;; Controls
  :button      JButton
  :label       JLabel)

;; Attributes

(defmethod set-attr :default
  [c k args]
  "Fall-back method implementation for properties not specified 
  for the component in defattributes.
  Tries to set the property by building a function setter
  and calling it with the supplied args."
  (let [ctrl  (impl c)
        args  (if (sequential? args) args [args])
        args  (map #(if (ui/component? %) (impl %) %) args)
        n     (count args)]
    (apply (swutil/setter! (class ctrl) k n) ctrl args)
    c))

;; Definition of attribute setters for each kind
;; of component in the hierarchy.
(swutil/defattributes
  :component
    (:border [c _ v]
      (let [v (if (sequential? v) v [v])]
        (.setBorder (impl c) (apply swutil/border v))))
    (:background [c _ v]
      (.setBackground (impl c) (swutil/color v)))
    (:foreground [c _ v]
      (.setForeground (impl c) (swutil/color v)))
    (:font [c _ value]
      (.setFont (impl c) (swutil/font value)))
    (:preferred-size [c attr [w h :as value]]
      (.setPreferredSize (impl c) (Dimension. w h)))

    (:on-click [c _ handler]
      (let [listener (proxy [MouseAdapter] []
                       (mousePressed [e]
                         (when (= 1 (.getClickCount e)) (handler e))))]
        (.addMouseListener (impl c) listener)))
    (:on-dbl-click [c _ handler]
      (let [listener (proxy [MouseAdapter] []
                       (mousePressed [e]
                         (when (= 2 (.getClickCount e)) (handler e))))]
        (.addMouseListener (impl c) listener)))

  :window
    (:menu [c _ v]
      (set-attr c :j-menu-bar (impl v))
      (.revalidate (impl c)))
    (:icons [c _ v]
      (let [icons (map swutil/image v)]
        (.setIconImages (impl c) icons)))

  :button
    (:icon [c _ img]
      (.setIcon (impl c) (swutil/icon img)))
    (:on-click [c _ f]
      (let [action (reify ActionListener
                      (actionPerformed [this e] (f e)))]
        (.addActionListener (impl c) action)))

  :split
    (:divider-location [c _ value]
      (.setDividerLocation (impl c) value))
    (:orientation [c attr value]
      (.setOrientation (impl c) (swutil/split-orientations value))))
