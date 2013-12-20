(ns lab.ui.swing.tab
  (:use     [lab.ui.protocols :only [Component abstract impl Selected selected to-map]])
  (:require [lab.ui.core :as ui])
  (:import  [javax.swing JTabbedPane JScrollPane JPanel]
            [javax.swing.event ChangeListener ChangeEvent]))

(defn- tab-initialize [c]
  (if (ui/attr c :scroll)
    (JScrollPane.)
    (doto (JPanel.)
      (.setLayout (java.awt.BorderLayout.)))))

(ui/definitializations
  :tabs        JTabbedPane
  :tab         tab-initialize)

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
  (:scroll [c _ _])

  :tabs
  (:on-tab-change [c _ handler]
    (let [listener (proxy [ChangeListener] []
                     (stateChanged [e] (handler (to-map e))))]
      (.addChangeListener (impl c) listener))))
