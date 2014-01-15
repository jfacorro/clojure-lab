(ns lab.ui.swing.text
  (:import  [javax.swing JTextArea JTextPane JScrollPane]
            [javax.swing.text JTextComponent Document]
            [javax.swing.event DocumentListener DocumentEvent DocumentEvent$EventType CaretListener]
            [javax.swing.text DefaultStyledDocument StyledDocument SimpleAttributeSet Highlighter$HighlightPainter]
            [java.awt.event ActionListener])
  (:use     [lab.ui.protocols :only [impl Event to-map TextEditor]])
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

(defn- reset-highlight [text hl last-view]
  (when @last-view
    (ui/action
      (let [offset     (.getCaretPosition text)
            cur-view   (.modelToView text offset)
            last-y     (.y @last-view)
            cur-y      (.y cur-view)]
        (when (not= last-y cur-y)
          (.repaint text 0 last-y (.getWidth text) (.height @last-view))
          (reset! last-view cur-view))))))

;; Code taken from http://tips4java.wordpress.com/2008/10/29/line-painter/
(defn- line-highlighter
  [text color]
  (let [last-view  (atom nil)
        hl         (proxy [Highlighter$HighlightPainter CaretListener] []
                     (paint [g p0 p1 bounds c]
                       (let [r (.modelToView c (.getCaretPosition c))]
                         (.setColor g color)
                         (.fillRect g 0 (.y r) (.getWidth c) (.height r))
                         (when-not @last-view
                           (reset! last-view r))))
                     (caretUpdate [e]
                       (reset-highlight text this last-view)))]
    (-> text .getHighlighter (.addHighlight 0 0 hl))
    (.addCaretListener text hl)
    text))

(defn- text-editor-init [c]
  (line-highlighter (JTextPane.) (util/color 0x444444)))

(ui/definitializations
  :text-area   JTextArea
  :text-editor #'text-editor-init)

(ui/defattributes
  :text-area
    (:text [c _ v]
      (.setText ^JTextComponent (impl c) v)
      ; I'm commenting this since it reset highlights added 
      ; to the highlighter, and can't remember why I added it :S
      #_(.updateUI (impl c)))
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
      (.setContentType ^JTextPane (impl c) v)))
