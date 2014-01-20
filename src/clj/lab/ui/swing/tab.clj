(ns lab.ui.swing.tab
  (:use     [lab.ui.protocols :only [Component abstract impl Selected selected to-map]])
  (:require [lab.ui.core :as ui])
  (:import  [javax.swing JTabbedPane JScrollPane JPanel UIManager]
            [javax.swing.event ChangeListener ChangeEvent]
            [java.awt Insets Color]))

(defn- set-prop [m k v]
  (doto m
    (.remove k)
    (.put k v)))

(-> (UIManager/getDefaults)
  (set-prop "TabbedPane.tabAreaInsets" (Insets. 0 0 0 0))
  (set-prop "TabbedPane.tabInsets" (Insets. 0 0 0 0))
  (set-prop "TabbedPane.selectedTabPadInsets" (Insets. 0 0 0 0))
  (set-prop "TabbedPane.contentBorderInsets" (Insets. 0 0 0 0))
  
  (set-prop "TabbedPane.tabsOverlapBorder" true)
  (set-prop "TabbedPane.selectionFollowsFocus" true)
  (set-prop "TabbedPane.opaque" false)
  (set-prop "TabbedPane.tabsOpaque" false)

  (set-prop "TabbedPane.labelShift" 0)
  (set-prop "TabbedPane.textIconGap" 0)
  (set-prop "TabbedPane.tabRunOverlay" 0)
  (set-prop "TabbedPane.selectedLabelShift" 0)

  (set-prop "TabbedPane.darkShadow" (Color. 0 0 0 0))  
  (set-prop "TabbedPane.highlight" (Color. 0 0 0 0))
  (set-prop "TabbedPane.light" (Color. 0 0 0 0))
  (set-prop "TabbedPane.shadow" (Color. 0 0 0 0))
  
  (set-prop "TabbedPane.borderHightlightColor" (Color. 0 0 0 0))
  (set-prop "TabbedPane.selectHighlight" (Color. 0 0 0 0))
  (set-prop "TabbedPane.background" (Color. 0 0 0 0))
  (set-prop "TabbedPane.foreground" (Color. 0 0 0 0))
  
  (set-prop "TabbedPane.unselectedBackground" (Color. 0 0 0 0))
  (set-prop "TabbedPane.selected" (Color. 0 0 0 0))
  (set-prop "TabbedPane.tabAreaBackground" (Color. 0 0 0 0))
  (set-prop "TabbedPane.focus" (Color. 0 0 0 0))
  (set-prop "TabbedPane.contentAreaColor" (Color. 0 0 0 0)))

(defn- tab-init [c]
  (doto (JPanel.)
    (.setLayout (java.awt.BorderLayout.))))

(ui/definitializations
  :tabs        JTabbedPane
  :tab         tab-init)

(extend-protocol Component
  JTabbedPane
  (children [this child]
    (.getComponents this))
  (add [this child]
    (let [i         (.getTabCount this)
          child-abs (abstract child)
          header    (when-let [h (ui/attr child-abs :header)] (impl h))
          tool-tip  (ui/attr child-abs :tool-tip)
          title     (ui/attr child-abs :title)]
      (.addTab this title child)
      (when header (.setTabComponentAt this i header))
      (when tool-tip (.setToolTipTextAt this i tool-tip))
      (selected this i))
    this)
  (remove [this child]
    (.remove this child)
    this))

(extend-protocol Selected
  JTabbedPane
  (selected 
    ([this]
      (let [index (.getSelectedIndex this)]
        (when (<= 0 index)
          (-> (.getComponentAt this index)
            abstract
            (ui/attr :id)))))
    ([this index]
      (.setSelectedIndex this index))))

(ui/defattributes
  :tab
  (:title [c _ _])
  (:tool-tip [c _ _])
  (:header [c _ _])

  :tabs
  (:on-tab-change [c _ f]
    (let [listener (proxy [ChangeListener] []
                     (stateChanged [e] (ui/handle-event f e)))]
      (.addChangeListener ^JTabbedPane (impl c) listener))))
