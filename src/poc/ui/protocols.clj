(ns poc.ui.protocols)

(defprotocol Component
  (add [this child]))

(defmulti ;"Creates a component instance based on its :tag."
  create :tag)

(defmulti ;"Sets the attribute value for this component."
  set-attr
  (fn [{tag :tag} k _]
    [tag k]))

(declare create-component)

(defn add-children [component children]
  (reduce add component (map create-component children)))
  
(defn create-component
  [{:keys [tag content] :as component}]
  (when component
    (-> component
      create
      (add-children content))))
  
;; constructor functions
(defn window [& {:as m}]
  (merge {:tag :window} m))

(defn menu [& {:as m}]
  (merge {:tag :menu} m))
  
(defn menu-item [& {:as m}]
  (merge {:tag :menu} m))
  
(defn text [& {:as m}]
  (merge {:tag :menu} m))

