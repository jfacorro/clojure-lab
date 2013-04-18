(ns poc.ui.swing
  (:use [poc.ui.protocols :only [Component create create-component]]))

(extend-type java.awt.Container
  Component
  (add [this child]
    (.add this child)
    this))

;; Window
(defmethod create :window [{:keys [title size menu content]}]
  (doto (javax.swing.JFrame. title)
    (.setSize (first size) (second size))
    (.setJMenuBar (create-component menu))
    (.setVisible true)))

;; Menu Components
(defmethod create :menu-bar [component]
  (javax.swing.JMenuBar.))

(defmethod create :menu [{:keys [title]}]
  (javax.swing.JMenu. title))
      
(defmethod create :menu-item [{:keys [title]}]
  (javax.swing.JMenuItem. title))

;; Tabbed Component
(defmethod create :tabs [component]
  (javax.swing.JTabbedPane.))

;; Text Editor
(defmethod create :text-editor [component]
  (javax.swing.JTextPane.))