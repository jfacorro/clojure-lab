(ns lab.ui.swing.text
  (:import  [javax.swing JTextArea JTextPane JScrollPane]
            [javax.swing.text JTextComponent Document]
            [javax.swing.event DocumentListener DocumentEvent DocumentEvent$EventType]
            [java.awt.event ActionListener]
            [javax.swing.text DefaultStyledDocument StyledDocument SimpleAttributeSet])
  (:use     [lab.ui.protocols :only [impl Event to-map Text]])
  (:require [lab.model.document :as doc]
            [lab.ui.core :as ui]
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
  ([^javax.swing.JTextPane txt ^SimpleAttributeSet stl]
    (.setCharacterAttributes txt stl true)))

(extend-protocol Text
  JTextPane
  (text [this]
    (as-> (.getDocument this) doc
      (.getText doc 0 (.getLength doc))))
  (apply-style
    ([this start length style]
      (apply-style (.getDocument this) start length (util/make-style style)))
    ([this regions styles]
      (let [styles (reduce (fn [m [k v]] (assoc m k (util/make-style v))) styles styles)
            doc    (.getDocument this)
            blank  (DefaultStyledDocument.)
            pos    (.getCaretPosition this)]
        (.setDocument this blank)
        (doseq [[start length tag] regions]
          (.setCharacterAttributes ^DefaultStyledDocument doc 
            ^long start
            ^long length
            ^SimpleAttributeSet (styles tag (:default styles))
            true))
         (.setDocument this doc)
         (.setCaretPosition this pos)))))

(defn init-text-editor [c]
  (let [doc (ui/attr c :doc)]
    (if doc
      (doto (JTextPane.)
        (.setText (doc/text @doc))
        (.setCaretPosition 0))
      (JTextPane.))))

(ui/definitializations
  :text-area   JTextArea
  :text-editor init-text-editor
  :scroll-text-editor JScrollPane)

(ui/defattributes
  :text-area
    (:text [c _ v]
      (.setText ^JTextComponent (impl c) v))
    (:read-only [c _ v]
      (.setEditable ^JTextComponent (impl c) (not v)))
  :text-editor
    (:wrap [c _ _])
    (:doc [c _ doc])
    (:caret-color [c _ v]
      (.setCaretColor ^JTextPane (impl c) (util/color v)))
    (:on-change [c p handler]
      (let [listener (proxy [DocumentListener] []
                       (insertUpdate [e] (handler (to-map e)))
                       (removeUpdate [e] (handler (to-map e)))
                       (changedUpdate [e] #_(handler (to-map e))))
            doc      (.getDocument ^JTextPane (impl c))]
        (.addDocumentListener ^Document doc listener))))
