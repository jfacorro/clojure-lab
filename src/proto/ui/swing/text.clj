(ns proto.ui.swing.text
  (:import [javax.swing JTextField JTextPane JTextArea]
           [javax.swing.text DefaultStyledDocument StyledDocument]
           [javax.swing.event DocumentEvent DocumentEvent$EventType])
  (:require [proto.ui.protocols :as p]))

(defn insertion? 
  "Evaluates if e is an insertion event."
  [e]
  (-> e (.getType) (= DocumentEvent$EventType/INSERT)))

(extend-type DocumentEvent
  p/TextEdit
    (insertion? [this] 
      (-> this (.getType) (= DocumentEvent$EventType/INSERT)))
    (offset [this]
      (.getOffset this))
  p/Text
    (length [this]
      (.getLength this))
    (text [this]
      (if (insertion? this)
        (let [off (p/offset this)
              len (p/length this)]
          (.. this getDocument (getText off len)))
        "")))

(extend-type JTextPane
  p/Text
    (length [this]
      (-> this (.getDocument) p/length))
    (text [this]
      (-> this (.getDocument) p/text)))

(extend-type StyledDocument
  p/Text
    (length [this]
      (.getLength this))
    (text [this]
      (.getText this 0 (p/length this))))
