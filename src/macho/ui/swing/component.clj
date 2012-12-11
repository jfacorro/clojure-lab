(ns macho.ui.swing.component
  (:import  [java.awt Component]
            [java.awt.event MouseWheelListener KeyAdapter ActionListener]
            [javax.swing.event CaretListener DocumentListener UndoableEditListener DocumentEvent$EventType]
            [javax.swing.undo UndoManager])
    (:use   [macho.ui.protocols]
            [macho.ui.swing.util :as util]))

(defn comp-show! [w]
  (.setVisible w true))

(defn comp-hide! [w]
  (.setVisible w false))

(defn comp-add! [w c]
  (.add w c))

(def base-component
  {:add! comp-add!
   :set-attr! (fn [& args] (apply comp-set-attr! args))
   :show! comp-show!
   :hide! comp-hide!})

(extend Component
  UIComponent base-component)

(defn size! [c w h]
  "Sets the size of the window."
  (.setSize c w h))

(defn size [c]
  "Gets the size of the window."
  (let [s (.getSize c)
        w (.width s)
        h (.height s)]
    [w h]))

(defn font! [c f]
  "Sets the font of the component."
  (.setFont c f))

(defn font [c ]
  "Gets the font of the component."
  (.getFont c))
;;-----------------------------------------------------
(defn check-key 
  "Checks if the key and the modifier match the event's values"
  [evt k m]  
  (and 
    (or (nil? k) (= k (.getKeyCode evt)))
    (or (nil? m) (= m (.getModifiers evt)))))
;;-----------------------------------------------------
(defn process-event-handler [hdlr]
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
