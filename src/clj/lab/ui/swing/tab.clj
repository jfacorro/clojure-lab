(ns lab.ui.swing.tab
  (:use     [lab.ui.protocols :only [Component abstract impl Selection selection to-map listen ignore]])
  (:require [lab.ui.core :as ui]
            [lab.ui.util :refer [defattributes definitializations]]
            [lab.ui.swing.util :as util])
  (:import  [javax.swing JTabbedPane JScrollPane JPanel JComponent 
                         UIManager UIDefaults]
            [java.awt Insets Color]))

(defn- set-prop [^UIDefaults m k v]
  (doto m
    (.remove k)
    (.put k v)))

(def transparent (Color. 0 0 0 0))

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

  (set-prop "TabbedPane.darkShadow" transparent)
  (set-prop "TabbedPane.highlight" transparent)
  (set-prop "TabbedPane.light" transparent)
  (set-prop "TabbedPane.shadow" transparent)

  (set-prop "TabbedPane.borderHightlightColor" transparent)
  (set-prop "TabbedPane.selectHighlight" transparent)
  (set-prop "TabbedPane.background" transparent)
  (set-prop "TabbedPane.foreground" transparent)
  
  (set-prop "TabbedPane.unselectedBackground" transparent)
  (set-prop "TabbedPane.selected" transparent)
  (set-prop "TabbedPane.tabAreaBackground" transparent)
  (set-prop "TabbedPane.focus" transparent)
  (set-prop "TabbedPane.contentAreaColor" transparent))

(defn- style-tab-header [tab style]
  (reduce-kv ui/attr (ui/attr tab :header) style))

(defn- highlight-current-tab [e]
  (let [{:keys [source]} (to-map e)
        sel-style        (ui/attr source :selected-tab-style)
        unsel-style      (ui/attr source :unselected-tab-style)
        sel-id           (selection source)]
    (doseq [tab (ui/children source)]
      (style-tab-header tab unsel-style))
    (ui/update source (ui/id= sel-id) style-tab-header sel-style)))

(defn- tab-init [c]
  (doto (JPanel.)
    (.setLayout (java.awt.BorderLayout.))))

(defn- tabs-init [c]
  (doto (JTabbedPane.)
    (.setTabLayoutPolicy JTabbedPane/WRAP_TAB_LAYOUT)
    (.addChangeListener (util/create-listener c :change #'highlight-current-tab))))

(definitializations
  :tabs        tabs-init
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
      (util/remove-focus-traversal child)
      (when header (.setTabComponentAt this i header))
      (when tool-tip (.setToolTipTextAt this i tool-tip))
      (selection this i))
    this)
  (remove [this child]
    (.remove this ^JComponent child)
    this))

(extend-protocol Selection
  JTabbedPane
  (selection 
    ([this]
      (let [index (.getSelectedIndex this)]
        (when (<= 0 index)
          (-> this
            (.getComponentAt index)
            abstract
            (ui/attr :id)))))
    ([this index]
      (.setSelectedIndex this index))))

(defattributes
  :tabs
  (:selected-tab-style [c _ _])
  (:unselected-tab-style [c _ _])

  :tab
  (:title [c _ _])
  (:tool-tip [c _ _])
  (:header [c _ _]))

(defmethod listen [:tabs :change]
  [c evt f]
  (let [listener (util/create-listener c evt f)]
      (.addChangeListener ^JTabbedPane (impl c) listener)))

(defmethod ignore [:tabs :change]
  [c _ listener]
  (.removeChangeListener ^JTabbedPane (impl c) listener))
