(ns macho.ui.swing.component
  (:import  [java.awt Component]
            [java.awt.event MouseWheelListener KeyAdapter ActionListener]
            [javax.swing.event CaretListener DocumentListener UndoableEditListener DocumentEvent$EventType]
            [javax.swing.undo UndoManager])
    (:use   [macho.ui.protocols]
            [macho.ui.swing.util :as util]))
;;-----------------------------------------------------
(defn check-key 
  "Checks if the key and the modifier match the event's values"
  [evt k m]  
  (and 
    (or (nil? k) (= k (.getKeyCode evt)))
    (or (nil? m) (= m (.getModifiers evt)))))
;;-----------------------------------------------------
(defn process-event-handler [hdlr]
  "If the handler is a function of arity 0 then
it wraps it in a function of arity 1."
  (let [n (-> hdlr class .getDeclaredMethods first .getParameterTypes alength)]
    (if (zero? n) 
      #(do % (hdlr)) 
      hdlr)))
;;-----------------------------------------------------
;; Multi method implementation for mouse-wheel events.
;;-----------------------------------------------------
(defmethod on :mouse-wheel [evt ctrl hdlr]
  (let [pxy (proxy [MouseWheelListener] []
              (mouseWheelMoved [e] (hdlr e)))]
    (.addMouseWheelListener ctrl pxy)))
;;-----------------------------------------------------
;; Multi method implementation for key-press events.
;;-----------------------------------------------------
(defmethod on :key-press [evt ctrl hdlr & [key mask]]
  (.addKeyListener ctrl
      (proxy [KeyAdapter] []
        (keyPressed [e] (when (check-key e key mask) (hdlr e))))))
;;-----------------------------------------------------
;; Multi method implementation for key-release events.
;;-----------------------------------------------------
(defmethod on :key-release [evt ctrl hdlr & [key mask]]
  (.addKeyListener ctrl
    (proxy [KeyAdapter] []
      (keyReleased [e] (when (check-key e key mask) (hdlr e))))))
;;-----------------------------------------------------
;; Multi method implementation for click events.
;;-----------------------------------------------------
(defmethod on :click [evt ctrl hdlr & [key mask]]
  (let [f (process-event-handler hdlr)]
    (.addActionListener ctrl
      (proxy [ActionListener] []
        (actionPerformed [e] (f e))))))
;;-----------------------------------------------------
;; Multi method implementation for caret-update events.
;;-----------------------------------------------------
(defmethod on :caret-update [evt ctrl hdlr]
  (let [f (process-event-handler hdlr)]
    (.addCaretListener ctrl
      (proxy [CaretListener] []
        (caretUpdate [e] (f e))))))
;;-----------------------------------------------------
;; Multi method implementation for doc-change events.
;;-----------------------------------------------------
(defmethod on :change [evt ctrl hdlr]
  (let [f (process-event-handler hdlr)
        doc (.getDocument ctrl)]
    (.addDocumentListener doc
      (proxy [DocumentListener] []
        (changedUpdate [e] nil)
        (insertUpdate [e] (queue-action #(f e)))
        (removeUpdate [e] (queue-action #(f e)))))))
;;-----------------------------------------------------
;; Multi method implementation for doc-change events.
;;-----------------------------------------------------
(defmethod on :undoable  [evt ctrl hdlr]
  (let [f (process-event-handler hdlr)]
    (.addUndoableEditListener ctrl
      (proxy [UndoManager] []
        (undoableEditHappened [e] (queue-action #(f e)))))))
;;-----------------------------------------------------
