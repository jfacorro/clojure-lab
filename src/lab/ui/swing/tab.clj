(ns lab.ui.swing.Tab
  (:gen-class
     :extends javax.swing.JScrollPane
     :state state
     :init  init
     :constructors {[String] [], [] []}
     :methods [[setTitle [String] void]
               [getTitle [] String]
               [setHeader [Object] void]
               [getHeader [] Object]]))

(defn -init
  ([]
    (-init nil))
  ([title]
    [[] (atom {:title title})]))

(defn set-property [this prop value]
  (swap! (.state this) assoc prop value))
  
(defn get-property [this prop]
  (prop @(.state this)))

(defn -setTitle [this value]
  (set-property this :title value))

(defn -getTitle [this]
  (get-property this :title))

(defn -setHeader [this value]
  (set-property this :header value))

(defn -getHeader [this]
  (get-property this :header))

(compile 'lab.ui.swing.Tab)