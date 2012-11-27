(ns macho.ui.swing.UndoManager
  (:import [javax.swing.undo UndoManager UndoableEdit CompoundEdit]
           [javax.swing.event UndoableEditEvent DocumentEvent$EventType])
  (:gen-class
    :name    "macho.ui.swing.UndoManager"
    :extends javax.swing.undo.UndoManager
    ;:exposes-methods {undo undoSuper}
    :main    false
    :state   state
    :constructors {[Object Object] []}
    :init    init))
;------------------------------------
; Getter and setter
;------------------------------------
(defn set-field [^macho.ui.swing.UndoManager this ^clojure.lang.Keyword field value]
  (let [state (.state this)]
    (swap! state #(into % {field value}))))
;------------------------------------
(defn get-field [^macho.ui.swing.UndoManager this ^clojure.lang.Keyword field]
  (let [state (.state this)]
    (field @state)))
;------------------------------------
(defn get-component [^macho.ui.swing.UndoManager this]
  (get-field this :component))
;------------------------------------
(defn get-document [^macho.ui.swing.UndoManager this]
  (get-field this :document))
;------------------------------------
(defn get-edit [^macho.ui.swing.UndoManager this]
  (get-field this :edit))
;------------------------------------
(defn set-edit [^macho.ui.swing.UndoManager this value]
  (set-field this :edit value))
;------------------------------------
(defn set-offset [^macho.ui.swing.UndoManager this value]
  (set-field this :offset value))
;------------------------------------
(defn get-offset [^macho.ui.swing.UndoManager this]
  (get-field this :offset))
;------------------------------------
(defn set-length [^macho.ui.swing.UndoManager this value]
  (set-field this :length value))
;------------------------------------
(defn get-length [^macho.ui.swing.UndoManager this]
  (get-field this :length))
;------------------------------------
(defn current-offset [^macho.ui.swing.UndoManager this]
  (let [cmpt (get-component this)]
    (.getCaretPosition cmpt)))
;------------------------------------
(defn current-length [^macho.ui.swing.UndoManager this]
  (let [doc (get-document this)]
    (.getLength doc)))
;------------------------------------
(defn update-length [^macho.ui.swing.UndoManager this]
  ;(set-offset this (current-offset this))
  (set-length this (current-length this)))
;------------------------------------
(defn -undoableEditHappened [^macho.ui.swing.UndoManager this ^UndoableEditEvent e]
  (let [edit      (.getEdit e)
        length    (- (current-length this) (get-length this))]
;    (when-not (.contains (.getPresentationName edit) "style")
;      (.addEdit this edit))
    (update-length this)
    (when (pos? (Math/abs length))
      (.addEdit this edit))))
;------------------------------------
(defn -init [component doc]
  [[] (atom {:offset 0 
             :length 0
             :component component 
             :document doc 
             :edit nil})])
;------------------------------------
