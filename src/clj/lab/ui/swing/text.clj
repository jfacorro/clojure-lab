(ns lab.ui.swing.text
  (:use     [lab.ui.protocols :only [impl abstract
                                     Event to-map
                                     listen
                                     TextEditor StyledTextEditor
                                     Selection selection caret-position]])
  (:require [lab.model.protocols :as mp]
            [lab.ui.core :as ui]
            [lab.ui.swing [util :as util]
                          [event :as event]])
  (:import  [lab.ui.swing TextLineNumber LineHighlighter]
            [javax.swing JTextArea JTextPane JTextField]
            [javax.swing.text JTextComponent Document]
            [javax.swing.event DocumentListener CaretListener]
            [javax.swing.text DefaultStyledDocument StyledDocument SimpleAttributeSet]
            [java.awt Color]))

(defn- apply-style
  "Applies the given style to the text
  enclosed between the strt and end positions."
  ([^StyledDocument doc ^long strt ^long end ^SimpleAttributeSet stl]
    (.setCharacterAttributes doc strt end stl true))
  ([^JTextPane txt ^SimpleAttributeSet stl]
    (.setCharacterAttributes txt stl true)))

(extend-type JTextComponent
  TextEditor
  (add-highlight [this start end color]
    (.. this getHighlighter (addHighlight start end (util/highlighter color))))
  (remove-highlight [this id]
    (.. this getHighlighter (removeHighlight id)))
  (caret-position
    ([this]
      (.getCaretPosition this))
    ([this position]
      (.setCaretPosition this position)))

  Selection
  (selection
    ([this]
      [(.getSelectionStart this) (.getSelectionEnd this)])
    ([this [start end]]
      (.setSelectionStart this start)
      (.setSelectionEnd this end)))
  
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

(def ^:private blank-document (DefaultStyledDocument.))

(defn- apply-style [^JTextPane this f]
  (let [doc    (.getDocument this)
        pos    (.getCaretPosition this)
        sel    (selection this)
        listeners (.getCaretListeners this)]
    ;; Remove caret listeners before assigning a blank document since caret is set to 0.
    (doseq [x listeners] (.removeCaretListener this x))
    (.setDocument this blank-document)
    (f doc)
    (.setDocument this doc)
    (caret-position this pos)
    (selection this sel)
    ;; Add caret listeners after resetting the caret position
    (doseq [x listeners] (.addCaretListener this x))))

(extend-type JTextPane
  StyledTextEditor
  (apply-style
    ([this regions styles]
      (let [styles (reduce-kv #(assoc %1 %2 (util/make-style %3)) styles styles)]
        (apply-style this
          #(doseq [[start length tag] regions]
            (.setCharacterAttributes ^DefaultStyledDocument % start length (styles tag (:default styles)) true)))))
    ([this start len style]
      (apply-style this
        #(.setCharacterAttributes ^DefaultStyledDocument % start len (util/make-style style) true)))))

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
  :text-field  JTextField
  :text-area   JTextArea
  :text-editor #'text-editor-init
  :line-number #'line-number-init)

(ui/defattributes
  :text-field
    (:text [c _ v]
      (.setText ^JTextComponent (impl c) v))
    (:read-only [c _ v]
      (.setEditable ^JTextComponent (impl c) (not v)))
    (:caret-color [c _ v]
      (.setCaretColor ^JTextComponent (impl c) (util/color v)))

  :text-area
    (:line-highlight-color [c _ _])

  :text-editor
    (:wrap [c _ _])
    (:doc [c _ _])
    (:content-type [c _ v]
      (.setContentType ^JTextPane (impl c) v))

  :line-number
    (:source [c _ _])
    (:update-font [c _ v]
       (.setUpdateFont ^TextLineNumber (impl c) v))
    (:border-gap [c _ v]
       (.setBorderGap ^TextLineNumber (impl c) v))
    (:current-line-color [c _ v]
       (.setCurrentLineForeground ^TextLineNumber (impl c) (util/color v))))


(defmethod listen [:text-field :caret]
  [c evt f]
  (let [listener  (util/create-listener c evt f)]
    (.addCaretListener ^JTextComponent (impl c) listener)
    listener))

(defmethod listen [:text-field :insert]
  [c evt f]
  (let [listener  (util/create-listener c evt f)
        doc      (.getDocument ^JTextComponent (impl c))]
    (.addDocumentListener ^Document doc listener)
    listener))

(defmethod listen [:text-field :delete]
  [c evt f]
  (let [listener  (util/create-listener c evt f)
        doc      (.getDocument ^JTextComponent (impl c))]
    (.addDocumentListener ^Document doc listener)
    listener))

(defmethod listen [:text-field :change]
  [c evt f]
  (let [listener  (util/create-listener c evt f)
        doc      (.getDocument ^JTextComponent (impl c))]
    (.addDocumentListener ^Document doc listener)
    listener))
