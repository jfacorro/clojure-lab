(ns lab.ui.swing
  (:refer-clojure :exclude [remove])
  (:import [javax.swing UIManager JFrame JMenuBar JMenu JMenuItem JTabbedPane 
                        JScrollPane JTextPane JTree JSplitPane JButton JPanel
                        JButton JLabel AbstractAction JComponent]
           [javax.swing.tree TreeNode DefaultMutableTreeNode DefaultTreeModel]           
           [javax.swing.event TreeSelectionListener DocumentListener]
           [java.awt Dimension]
           [java.awt.event MouseAdapter])
  (:use    [lab.ui.protocols :only [Component add remove
                                    initialize set-attr
                                    Abstract impl 
                                    Visible visible? hide show
                                    Implementation abstract
                                    Event source
                                    Selected get-selected set-selected]])
  (:require [lab.util :as util]
            [lab.ui.core :as ui]
            [lab.ui.swing.util :as swutil]))
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
          header    (-> child-abs (ui/get-attr :-header) impl)
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
    this)

  DefaultMutableTreeNode
  (add [this child]
    (.add this child)
    this))
;;-------------------
(extend-protocol Selected
  JTree 
  (get-selected [this]
    (when-let [node (-> this .getLastSelectedPathComponent)]
      (.getUserObject node)))
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
  ;; Menu
  :menu-bar    JMenuBar
  :menu        JMenu
  :menu-item   JMenuItem
  ;; Panels
  :tabs        JTabbedPane
  :tab         JScrollPane
  ;; Text
  :text-editor JTextPane
  ;; Layout
  :split       JSplitPane
  :panel       JPanel
  ;; Tree
  :tree        JTree
  :tree-node   DefaultMutableTreeNode
  ;; Controls
  :button      JButton
  :label       JLabel)

;; Attributes

(defmethod set-attr :default
  [c k args]
  "docstring: fall-back method implementation
  for properties not specified for the component.
  Tries to set the property by building a function setter
  and calling it with the supplied args."
  (let [ctrl  (impl c)
        args  (if (sequential? args) args [args])
        args  (map #(if (ui/component? %) (impl %) %) args)
        n     (count args)]
    (apply (swutil/setter! (class ctrl) k n) ctrl args)
    c))

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
      (.setJMenuBar (impl c) (impl v)))
    (:icons [c _ v]
      (let [icons (map swutil/image v)]
        (.setIconImages (impl c) icons)))

  :tree
    (:root [c _ v]
      (let [model (DefaultTreeModel. (impl v))]
        (.setModel (impl c) model)))
    (:on-selected [c _ handler]
      (let [listener (proxy [TreeSelectionListener] []
                       (valueChanged [e]
                         (handler (get-selected c))))]
        (.addTreeSelectionListener (impl c) listener)))

  :tree-node
    (:item [c attr item]
      (.setUserObject (impl c) item))

  :split
    (:orientation [c attr value]
      (.setOrientation (impl c) (swutil/split-orientations value)))

  :button 
    (:on-click [c _ f]
      (let [action (proxy [AbstractAction] []
                      (actionPerformed [e] (f c)))]
        (.setAction (impl c) action)))

  :text-editor
    (:caret-color [c _ v]
      (.setCaretColor (impl c) (swutil/color v)))
    (:on-insert [c _ handler]
      (let [listener (proxy [DocumentListener] []
                       (insertUpdate [e] (handler e))
                       (removeUpdate [e])
                       (changedUpdate [e]))
            doc      (.getDocument (impl c))]
        (.addDocumentListener doc listener)))
    (:on-delete [c _ handler]
      (let [listener (proxy [DocumentListener] []
                       (insertUpdate [e])
                       (removeUpdate [e] (handler e))
                       (changedUpdate [e]))
            doc      (.getDocument (impl c))]
        (.addDocumentListener doc listener)))
    (:on-change [c _ handler]
      (let [listener (proxy [DocumentListener] []
                       (insertUpdate [e])
                       (removeUpdate [e])
                       (changedUpdate [e] (handler e)))
            doc      (.getDocument (impl c))]
        (.addDocumentListener doc listener))))
