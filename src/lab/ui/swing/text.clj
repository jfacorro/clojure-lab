(ns lab.ui.swing.text
  (:import  [javax.swing JTextPane]
            [javax.swing.event DocumentListener DocumentEvent DocumentEvent$EventType]
            [java.awt.event ActionListener])
  (:use     [lab.ui.protocols :only [impl Event to-map Text]])
  (:require [lab.ui.core :as ui]
            [lab.ui.swing [util :as util]
                          [event :as event]]))

(def ^:private event-types
  {DocumentEvent$EventType/INSERT  :insert
   DocumentEvent$EventType/REMOVE  :remove
   DocumentEvent$EventType/CHANGE  :change})

(extend-protocol Event
  DocumentEvent
  (to-map [this]
    (let [offset     (.getOffset this)
          length     (.getLength this)
          doc        (.getDocument this)
          event-type (event-types (.getType this))
          text       (when (not= event-type :remove)
                       (.getText doc offset length))]
      {:offset   offset
       :length   length
       :text     text
       :type     event-type
       :document doc})))

(extend-protocol Text
  JTextPane
  (text [this]
    (as-> (.getDocument this) doc 
      (.getText doc 0 (.getLength doc))))
  (apply-style [this start length style]
    (.setCharacterAttributes (.getDocument this) 
      start
      length
      (util/make-style style)
      true)))

(ui/definitializations
  :text-editor JTextPane)

(ui/defattributes
  :text-editor
    (:wrap [c _ _])
    (:doc [c _ _])
    (:text [c _ v]
      (.setText (impl c) v))
    (:caret-color [c _ v]
      (.setCaretColor (impl c) (util/color v)))
    (:on-change [c _ handler]
      (let [listener (proxy [DocumentListener] []
                       (insertUpdate [e] (handler (to-map e)))
                       (removeUpdate [e] (handler (to-map e)))
                       (changedUpdate [e] (handler (to-map e))))
            doc      (.getDocument (impl c))]
        (.addDocumentListener doc listener))))
