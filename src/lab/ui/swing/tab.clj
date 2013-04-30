(ns lab.ui.swing.Tab
  (:gen-class
     :extends javax.swing.JScrollPane
     :state state
     :init  init
     :constructors {[String] [], [] []}
     :methods [[setTitle [String] void]
               [getTitle [] String]]))

(defn -init
  ([]
    (-init nil))
  ([title]
    [[] (atom {:title title})]))

(defn set-property [this prop value]
  (swap! (.state this) assoc :title value))
  
(defn get-property [this prop]
  (prop @(.state this)))

(defn -setTitle [this title]
  (set-property this :title title))

(defn -getTitle [this]
  (get-property this :title))
