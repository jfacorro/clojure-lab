(ns lab.ui.swing.text
  (:use     [lab.ui.protocols :only [impl Event to-map TextEditor abstract]])
  (:require [lab.model.protocols :as mp]
            [lab.ui.core :as ui]
            [lab.ui.swing [util :as util]
                          [event :as event]])
  (:import  [lab.ui.swing TextLineNumber LineHighlighter]
            [javax.swing JTextArea JTextPane]
            [javax.swing.text JTextComponent Document]
            [javax.swing.event DocumentListener CaretListener]
            [javax.swing.text DefaultStyledDocument StyledDocument SimpleAttributeSet]
            [java.awt Color]))

(set! *warn-on-reflection* true)

(defn- apply-style
  "Applies the given style to the text
  enclosed between the strt and end positions."
  ([^StyledDocument doc ^long strt ^long end ^SimpleAttributeSet stl]
    (.setCharacterAttributes doc strt end stl true))
  ([^JTextPane txt ^SimpleAttributeSet stl]
    (.setCharacterAttributes txt stl true)))

(extend-type JTextArea
  TextEditor
  (apply-style [this regions styles]
    (throw (Exception. "Should try to add style to a :text-area.")))
  (caret-position
    ([this]
      (.getCaretPosition this))
    ([this position]
      (.setCaretPosition this position))))


(extend-type JTextPane
  TextEditor
  (apply-style
    ([this regions styles]
      (let [styles (reduce-kv #(assoc %1 %2 (util/make-style %3)) styles styles)
            doc    ^DefaultStyledDocument (.getDocument this)
            blank  (DefaultStyledDocument.)
            pos    (.getCaretPosition this)]
        (.setDocument this blank)
        (doseq [[start length tag] regions]
          (.setCharacterAttributes doc start length (styles tag (:default styles)) true))
        (.setDocument this doc)
        (.setCaretPosition this pos))))
  (caret-position
    ([this]
      (.getCaretPosition this))
    ([this position]
      (.setCaretPosition this position)))

  mp/Text
  (insert [this offset s]
    (.insertString (.getDocument this) offset s nil)
    (.setCaretPosition this (+ offset (count s))))
  (delete [this start end]
    (.remove (.getDocument this) start (- end start))
    (.setCaretPosition this start))
  (length [this]
    (.getLength ^Document (.getDocument this)))
  (text [this]
    (.getText this))
  (substring [this start end]
    (-> this .getText (.substring start end))))

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
    (:on-caret [c _ f]
      (let [listener (proxy [CaretListener] []
                       (caretUpdate [e] (ui/handle-event f e)))]
        (.addCaretListener ^JTextPane (impl c) listener)))
    (:on-change [c _ f]
      (let [listener (proxy [DocumentListener] []
                       (insertUpdate [e] (ui/handle-event f e))
                       (removeUpdate [e] (ui/handle-event f e))
                       (changedUpdate [e] (ui/handle-event f e)))
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
       (.setUpdateFont ^TextLineNumber (impl c) v))
    (:border-gap [c _ v]
       (.setBorderGap ^TextLineNumber (impl c) v))
    (:curren-line-color [c _ v]
       (.setCurrentLineForeground ^TextLineNumber (impl c) (util/color v))))
