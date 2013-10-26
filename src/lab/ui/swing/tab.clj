(ns lab.ui.swing.tab
  (:import  [javax.swing JTabbedPane JScrollPane])
  (:use     [lab.ui.protocols :only [Component abstract impl Selected selected]])
  (:require [lab.ui.core :as ui]))

(ui/definitializations
  :tabs        JTabbedPane
  :tab         JScrollPane)

(extend-protocol Component
  JTabbedPane
  (children [this child]
    (.getComponents this))
  (add [this child]
    (let [i         (.getTabCount this)
          child-abs (abstract child)
          header    (when-let [h (ui/get-attr child-abs :header)] (impl h))
          tool-tip  (ui/get-attr child-abs :tool-tip)
          title     (ui/get-attr child-abs :title)]
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
      (let [selected (.getComponentAt this (.getSelectedIndex this))]
        (abstract selected)))
    ([this index]
      (.setSelectedIndex this index))))

(ui/defattributes
  :tab
  (:title [c _ _] c)
  (:tool-tip [c _ _] c)
  (:doc [c _ _] c)
  (:header [c _ _] c))
