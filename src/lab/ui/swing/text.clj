(ns lab.ui.swing.text
  (:import  [javax.swing JTextPane]
            [javax.swing.event DocumentListener DocumentEvent DocumentEvent$EventType]
            [java.awt.event ActionListener]
            [javax.swing.text DefaultStyledDocument StyledDocument SimpleAttributeSet])
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

(defn- apply-style
  "Applies the given style to the text
  enclosed between the strt and end positions."
  ([^StyledDocument doc ^long strt ^long end ^SimpleAttributeSet stl]
    (.setCharacterAttributes doc strt end stl true))
  ([^JTextPane txt ^SimpleAttributeSet stl]
    (.setCharacterAttributes txt stl true)))

(extend-protocol Text
  JTextPane
  (text [this]
    (as-> (.getDocument this) doc 
      (.getText doc 0 (.getLength doc))))
  (apply-style [this start length style]
    (apply-style (.getDocument this) start length (util/make-style style))))

(ui/definitializations
  :text-editor JTextPane)

(ui/defattributes
  :text-editor
    (:wrap [c _ _])
    (:doc [c _ doc])
    (:text [c p v]
      (let [txt        ^JTextPane (impl c)
            doc        ^DefaultStyledDocument (.getDocument txt)
            blank      (DefaultStyledDocument.)]
        (.setDocument ^JTextPane txt blank)
        (.insertString doc 0 v nil)
        (.setDocument ^JTextPane txt doc)
        (.setCaretPosition ^JTextPane txt 0)))
    (:caret-color [c _ v]
      (.setCaretColor ^JTextPane (impl c) (util/color v)))
    (:on-change [c p handler]
      (let [listener (proxy [DocumentListener] []
                       (insertUpdate [e] (handler (to-map e)))
                       (removeUpdate [e] (handler (to-map e)))
                       (changedUpdate [e] (handler (to-map e))))
            doc      (.getDocument ^JTextPane (impl c))]
        (.addDocumentListener doc listener))))
