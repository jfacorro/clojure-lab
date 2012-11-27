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
(defn get-offset [this]
  (get-field this :offset))
;------------------------------------
(defn set-length [this value]
  (set-field this :length value))
;------------------------------------
(defn get-length [this]
  (get-field this :length))
;------------------------------------
(defn current-offset [this]
  (let [cmpt (get-component this)]
    (.getCaretPosition cmpt)))
;------------------------------------
(defn current-length [this]
  (let [doc (get-document this)]
    (.getLength doc)))
;------------------------------------
(defn create-edit [mgr]
  (proxy [CompoundEdit] []
    (isInProgress [] false)
    (undo []
      (when-let [edit (get-edit mgr)]
        (.end edit))
      (proxy-super undo)
      (set-edit mgr nil))))
;------------------------------------
(defn update-length [this]
  (set-offset this (current-offset this))
  (set-length this (current-length this)))
;------------------------------------
(defn start-edit [this ^UndoableEdit edit]
  (update-length this)
  (let [cmpnd-edit (create-edit this)]
    (.addEdit cmpnd-edit edit)
    (.addEdit this cmpnd-edit)
    (set-edit this cmpnd-edit)))
;------------------------------------
(defn -undoableEditHappened [this ^UndoableEditEvent e]
  (let [edit      (.getEdit e)
        evt-type  (.getType edit)
        cmpd-edit (get-edit this)
        offset    (- (current-offset this) (get-offset this))
        length    (- (current-length this) (get-length this))]
;    (when-not (.contains (.getPresentationName edit) "style")
;      (.addEdit this edit))
    (update-length this)
    (cond (nil? cmpd-edit)
            (start-edit this edit)
          (zero? length)
            (.addEdit cmpd-edit edit)
          :else 
            (do (.end cmpd-edit)
                (start-edit this edit)))))
;------------------------------------
(defn -init [component doc]
  [[] (atom {:offset 0 
             :length 0
             :component component 
             :document doc 
             :edit nil})])
;------------------------------------
