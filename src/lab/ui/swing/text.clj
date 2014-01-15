(ns lab.ui.swing.text
  (:import  [lab.ui.swing TextLineNumber]
            [javax.swing JTextArea JTextPane JScrollPane]
            [javax.swing.text JTextComponent Document]
            [javax.swing.event DocumentListener DocumentEvent DocumentEvent$EventType CaretListener]
            [javax.swing.text DefaultStyledDocument StyledDocument SimpleAttributeSet Highlighter$HighlightPainter]
            [java.awt.event ActionListener MouseListener MouseMotionListener]
            [java.awt Color])
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
  "Creates a highighter for the text component."
  [text color]
  (let [last-view  (atom nil)
        hl         (proxy [Highlighter$HighlightPainter CaretListener MouseListener MouseMotionListener] []
                     (paint [g p0 p1 bounds c]
                       (let [r (.modelToView c (.getCaretPosition c))]
                         (.setColor g color)
                         (.fillRect g 0 (.y r) (.getWidth c) (.height r))
                         (when-not @last-view
                           (reset! last-view r))))
                     (caretUpdate [e]
                       (reset-highlight text this last-view))
                     (mousePressed [e] (reset-highlight text this last-view))
                     (mouseClicked [e])
                     (mouseEntered [e])
                     (mouseExited [e])
                     (mouseReleased [e])
                     (mouseDragged [e] (reset-highlight text this last-view))
                     (mouseMoved [e]))]
    (-> text .getHighlighter (.addHighlight 0 0 hl))
    (.addCaretListener text hl)
    (.addMouseListener text hl)
    (.addMouseMotionListener text hl)
    text))

(defn color-int [factor v]
  (let [v (int (* factor v))]
    (if (< 255 v ) 255 v)))

(defn lighter-color [factor color]
  (Color. (->> color .getRed (color-int factor))
          (->> color .getGreen (color-int factor))
          (->> color .getBlue (color-int factor))))

(defn- line-number-init
  [c]
  (let [src       (ui/attr c :source)]
    (when (or (nil? src) (nil? (impl src)))
      (throw (Exception. "Source can't be null and has be initialized already.")))
    (TextLineNumber. (impl src))))

(defn- text-editor-init
  [c]
  (let [color (ui/attr c :line-highlight-color)
        text  (JTextPane.)]
    (line-highlighter text
      (if color
        (util/color color)
        (lighter-color 0.4 (.getSelectionColor text))))))

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
    (:source [c _ _]))
