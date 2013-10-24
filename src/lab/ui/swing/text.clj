(ns lab.ui.swing.text
  (:import  [javax.swing JTextPane]
            [javax.swing.event DocumentListener DocumentEvent DocumentEvent$EventType]
            [java.awt.event ActionListener])
  (:use     [lab.ui.protocols :only [impl Event to-map]])
  (:require [lab.ui.core :as ui]
            [lab.ui.swing.util :as util]))

(defn- text-editor-init [c]
  (proxy [JTextPane] []
    (getScrollableTracksViewportWidth []
      (if (ui/get-attr c :wrap)
        true
        (<= (.. this getUI (getPreferredSize this) width)
            (.. this getParent getSize width))))))

(def ^:private event-types
  {DocumentEvent$EventType/INSERT  :insert
   DocumentEvent$EventType/REMOVE  :remove
   DocumentEvent$EventType/CHANGE  :change})

(extend-protocol Event
  DocumentEvent
  (to-map [this]
    {:offset   (.getOffset this)
     :length   (.getLength this)
     :text     (.getText (.getDocument this) (.getOffset this) (.getLength this))
     :type     (event-types (.getType this))
     :document (.getDocument this)}))

(ui/definitializations
  :text-editor text-editor-init)

(ui/defattributes
  :text-editor
    (:caret-color [c _ v]
      (.setCaretColor (impl c) (util/color v)))
    (:on-change [c _ handler]
      (let [listener (proxy [DocumentListener] []
                       (insertUpdate [e] (handler (to-map e)))
                       (removeUpdate [e] (handler (to-map e)))
                       (changedUpdate [e] (handler (to-map e))))
            doc      (.getDocument (impl c))]
        (.addDocumentListener doc listener))))
