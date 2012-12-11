(ns macho.ui.swing.component
  (:import [java.awt Component]
           [java.awt.event MouseWheelListener])
  (:use [macho.ui.protocols]))

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
   :hide! comp-hide!
   :on comp-on})

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
;; Multi method implementation for mouse wheel events.
;;-----------------------------------------------------
(defmethod comp-on :mouse-wheel [ctrl evt hndlr]
  (let [pxy (proxy [MouseWheelListener] []
              (mouseWheelMoved [e] (hndlr e)))]
    (.addMouseWheelListener ctrl pxy)))

