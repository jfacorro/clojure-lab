(ns lab.ui.swing.text
  (:import  [javax.swing JTextPane]
            [javax.swing.event DocumentListener]
            [java.awt.event ActionListener])
  (:use     [lab.ui.protocols :only [impl]])
  (:require [lab.ui.core :as ui]
            [lab.ui.swing.util :as util]))

(defn- text-editor-init [c]
  (proxy [JTextPane] []
    (getScrollableTracksViewportWidth []
      (if (ui/get-attr c :wrap)
        true
        (<= (.. this getUI (getPreferredSize this) width)
            (.. this getParent getSize width))))))

(ui/definitializations
  :text-editor text-editor-init)

(ui/defattributes
  :text-editor
    (:caret-color [c _ v]
      (.setCaretColor (impl c) (util/color v)))
    (:on-insert [c _ handler]
      (let [listener (proxy [DocumentListener] []
                       (insertUpdate [e] (handler e))
                       (removeUpdate [e])
                       (changedUpdate [e]))
            doc      (.getDocument (impl c))]
        (.addDocumentListener doc listener)))
    (:on-delete [c _ handler]
      (let [listener (proxy [DocumentListener] []
                       (insertUpdate [e])
                       (removeUpdate [e] (handler e))
                       (changedUpdate [e]))
            doc      (.getDocument (impl c))]
        (.addDocumentListener doc listener)))
    (:on-change [c _ handler]
      (let [listener (proxy [DocumentListener] []
                       (insertUpdate [e])
                       (removeUpdate [e])
                       (changedUpdate [e] (handler e)))
            doc      (.getDocument (impl c))]
        (.addDocumentListener doc listener))))
