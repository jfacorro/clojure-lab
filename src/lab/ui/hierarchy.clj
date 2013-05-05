(ns lab.ui.hierarchy
  "Declares and builds the UI component hierarchy
  wich can be modified using alter-var-root! for 
  the hierarch var."
  )
  

(defn- process-hierarchy-entry
  "Takes a map entry from the hierarchy description
  and declares the parent/child relationship in the
  hierarchy being built."
  [h [parent children]]
  (reduce #(derive %1 %2 parent) h children))

(defn- build-hierarchy
  "Takes a map of parent/children specifications and
  builds the hierachy defined in it."
  [h]
  (reduce process-hierarchy-entry (make-hierarchy) h))

(def ^:private hierarchy-description
  "Default hierarchy description for the different types
  of UI components. Must be built and rebound to the result
  of calling [build-hierarchy]."
  {:component [:window :panel :scroll :tabs :tab :tree :split
               :tree-node :menu :menu-item :button :label :text-editor]})

(def hierarchy (build-hierarchy hierarchy-description))