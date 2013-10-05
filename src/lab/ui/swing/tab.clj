(ns lab.ui.swing.tab
  (:import  [javax.swing JTabbedPane JScrollPane])
  (:use     [lab.ui.protocols :only [Component abstract impl Selected set-selected]])
  (:require [lab.ui.core :as ui]))

(ui/definitializations
  :tabs        JTabbedPane
  :tab         JScrollPane)

(extend-protocol Component
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
    this))

(extend-protocol Selected
  JTabbedPane
  (get-selected [this]
    (.getSelectedIndex this))
  (set-selected [this index]
    (.setSelectedIndex this index)))