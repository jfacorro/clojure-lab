(ns macho.ui.swing.UndoManager
  (:import [javax.swing.undo UndoManager UndoableEdit CompoundEdit]
           [javax.swing.event UndoableEditEvent])
  (:gen-class
    :name    "macho.ui.swing.UndoManager"
    :extends javax.swing.undo.UndoManager
    :main    false
    :state   state
    :constructors {[Object Object] []}
    :init    init))
;------------------------------------
; Getter and setter
;------------------------------------
(defn set-field [this field value]
  (let [state (.state this)]
    (swap! state #(into % {field value}))))
;------------------------------------
(defn get-field [this field]
  (let [state (.state this)]
    (field @state)))
;------------------------------------
(defn get-component [this]
  (get-field this :component))
;------------------------------------
(defn get-document [this]
  (get-field this :document))
;------------------------------------
(defn get-edit [this]
  (get-field this :edit))
;------------------------------------
(defn set-edit [this value]
  (set-field this :edit value))
;------------------------------------
(defn set-offset [this value]
  (set-field this :offset value))
;------------------------------------
(defn set-length [this value]
  (set-field this :length value))
;------------------------------------
(defn current-offset [this]
  (let [cmpt (get-component this)]
    (.getCaretPosition cmpt)))
;------------------------------------
(defn current-length [this]
  (let [doc (get-document this)]
    (.getLength doc)))
;------------------------------------
(defn -undoableEditHappened [this ^UndoableEditEvent e]
  (let [edit      (.getEdit e)
        cmpd-edit (get-edit this)]
;    (when-not (.contains (.getPresentationName edit) "style")
;      (.addEdit this edit))
))
;------------------------------------
(defn start-edit [this ^UndoableEdit edit]
  (set-offset this (current-offset this))
  (set-length this (current-length this))
  (let [cmpnd-edit (CompoundEdit.)]
    (.addEdit cmpnd-edit edit)
    (.addEdit this cmpnd-edit)
    cmpnd-edit))
;------------------------------------
(defn -init [component doc]
  [[] (atom { :offset nil 
              :length nil
              :component component 
              :document doc 
              :edit nil})])
;------------------------------------


