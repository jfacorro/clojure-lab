(ns lab.ui.swing.text
  (:use     [lab.ui.protocols :only [impl Event to-map TextEditor abstract]])
  (:require [lab.ui.core :as ui]
            [lab.ui.swing [util :as util]
                          [event :as event]])
  (:import  [lab.ui.swing TextLineNumber LineHighlighter]
            [javax.swing JTextArea JTextPane]
            [javax.swing.text JTextComponent Document]
            [javax.swing.event DocumentListener DocumentEvent DocumentEvent$EventType CaretListener]
            [javax.swing.text DefaultStyledDocument StyledDocument SimpleAttributeSet]
            [java.awt Color]))

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
          editor     (.getProperty doc :component)
          event-type (event-types (.getType this))
          text       (when (not= event-type :remove)
                       (.getText doc offset length))]
      {:source   (abstract editor)
       :offset   offset
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

(extend-protocol TextEditor
  JTextPane
  (apply-style
    [this regions styles]
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
       (.setCaretPosition this pos))))

(defn- line-number-init [c]
  (let [src (ui/attr c :source)]
    (when-not (and src (impl src))
      (throw (Exception. "The source text component needs to be set and initialized.")))
    (TextLineNumber. (impl src))))

(defn- text-editor-init
  [c]
  (let [color (ui/attr c :line-highlight-color)
        text  (JTextPane.)
        doc   (.getDocument text)]
    ;; Add the text editor as a property of the document
    (.putProperty doc :component text)
    (if color
      (LineHighlighter. text (util/color color))
      (LineHighlighter. text))
    text))

(ui/definitializations
  :text-area   JTextArea
  :text-editor #'text-editor-init
  :line-number #'line-number-init)

(ui/defattributes
  :text-area
    (:text [c _ v]
      (.setText ^JTextComponent (impl c) v))
    (:line-highlight-color [c _ _])
    (:read-only [c _ v]
      (.setEditable ^JTextComponent (impl c) (not v)))
    (:caret-color [c _ v]
      (.setCaretColor ^JTextComponent (impl c) (util/color v)))
    (:caret-position [c _ v]
      (.setCaretPosition (impl c) v))
    (:on-caret [c _ f]
      (let [listener (proxy [CaretListener] []
                       (caretUpdate [e] (f (to-map e))))]
        (.addCaretListener ^JTextPane (impl c) listener)))
    (:on-change [c _ f]
      (let [listener (proxy [DocumentListener] []
                       (insertUpdate [e] (f (to-map e)))
                       (removeUpdate [e] (f (to-map e)))
                       (changedUpdate [e] (f (to-map e))))
            doc      (.getDocument ^JTextPane (impl c))]
        (.addDocumentListener ^Document doc listener)))
  :text-editor
    (:wrap [c _ _])
    (:doc [c _ doc])
    (:content-type [c _ v]
      (.setContentType ^JTextPane (impl c) v))
  :line-number
    (:source [c _ _])
    (:update-font [c _ v]
       (.setUpdateFont (impl c) v))
    (:border-gap [c _ v]
       (.setBorderGap (impl c) v))
    (:curren-line-color [c _ v]
       (.setCurrentLineForeground (impl c) (util/color v))))
