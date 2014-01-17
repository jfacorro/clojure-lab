(ns lab.ui.hierarchy
  "Declares and builds the UI component hierarchy
  which can be modified using alter-var-root! for 
  the hierarchy var.")

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
  {:component [; containers
                  :window :panel :split :scroll
                  ; menu
                  :menu-bar :menu :menu-item :menu-separator 
                  ; text
                  :text-area
                  :line-number
                  ; tabs
                  :tabs :tab
                  ; tree
                  :tree :tree-node
                  ; misc
                  :button :label
                  ; dialogs
                  :dialog]
    :dialog [:file-dialog :option-dialog]
    :text-area [:text-editor]})

(def hierarchy (build-hierarchy hierarchy-description))
