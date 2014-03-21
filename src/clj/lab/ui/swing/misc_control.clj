(ns lab.ui.swing.misc-control
  (:require [lab.ui.core :as ui]
            [lab.ui.util :refer [defattributes definitializations]]
            [lab.ui.protocols :refer [impl abstract
                                      Selection selection
                                      Component
                                      listen ignore]]
            [lab.ui.swing.util :as util])
  (:import [javax.swing JButton JLabel JCheckBox JComboBox]))

(defn- combobox-item-init
  [c]
  (let [abs  (atom nil)
        impl (proxy [Object lab.ui.protocols.Implementation] []
               (toString [] (ui/attr c :text))
               (abstract
                 ([] @abs)
                 ([x] (reset! abs x) this)))]
    impl))

(definitializations
  :button      JButton
  :label       JLabel
  :checkbox    JCheckBox
  :combobox    JComboBox
  :cb-item     combobox-item-init)

(defattributes
  :button
  (:text [c _ v]
    (.setText ^JButton (impl c) v))
  (:transparent [c _ v]
    (.setContentAreaFilled ^JButton (impl c) (not v)))
  (:icon [c _ img]
    (.setIcon ^JButton (impl c) (util/icon img)))

  :label
  (:text [c _ v]
    (.setText ^JLabel (impl c) v))

  :checkbox
  (:text [c _ v]
    (.setText ^JCheckBox (impl c) v))

  :combobox
  (:editable [c _ v]
    (.setEditable ^JComboBox (impl c) v))

  :cb-item
  (:text [c _ v]))

(extend-type JCheckBox
  Selection
  (selection
    ([this]
      (.isSelected this))
    ([this v]
      (.setSelected this v))))

(extend-type JComboBox
  Component
  (children [this] nil)
  (add [this child]
    (.addItem this child)
    this)
  (remove [this child]
    (.removeItem this child))
  (focus [this]
    (.grabFocus this)))

(defmethod listen [:button :click]
  [c evt f]
  (let [listener  (util/create-listener c evt f)]
    (.addActionListener ^JButton (impl c) listener)
    listener))

(defmethod ignore [:button :click]
  [c _ listener]
  (.removeActionListener ^JButton (impl c) listener))
