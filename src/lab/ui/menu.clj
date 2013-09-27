(ns lab.ui.menu
  (:require [lab.ui [core :as ui :reload true]
                    [select :as ui.sel :reload true]
                    [protocols :as uip]]))

(defn- menu-path
  "Deconstructs a menu path from a string with a '->' separator."
  [^String s]
  (when s
    (->> (.split s "->") seq (map clojure.string/trim))))

(defn- create-menu-path
  "Searches the menu-bar children using the selector. If the
  menu defined is not found it is created, otherwise the menu-bar 
  is returned unchanged."
  [menu-bar selector]
  (if (ui/find menu-bar selector)
    menu-bar
    (let [text     (-> selector last meta :value) ; The meta from the last selector's predicate has the name of the menu.
          menu     (ui/menu :text text)
          selector (or (butlast selector) [])]
      (ui/update menu-bar selector uip/add menu))))

(defn add-option
  "Takes a menu option and add it to the ui menu bar.
  The menu option map must have the following keys:
    :menu      -> path in the menu.
    :name      -> name of the option.
    :action    -> var to a function with args [ui evt & args].
    :separator -> true if the option should be followed by a separator."
  [ui {:keys [menu name action separator key-stroke] :as option}]
  (let [menu-bar  (ui/get-attr ui :menu)
                  ;; Explode the menu path and build a selector.
        selector  (map (partial ui.sel/attr= :text) (menu-path menu))
                  ;; Build selectors for each of the menu path levels.
        selectors (map #(->> selector (take %1) vec) (range 1 (-> selector count inc)))
        item      (if separator
                    (ui/menu-separator)
                    (ui/menu-item :text name :on-click (partial action ui) :key-stroke key-stroke))
        menu-bar  (reduce create-menu-path menu-bar selectors)
        menu-bar  (ui/update menu-bar selector uip/add item)]
     (ui/set-attr ui :menu menu-bar)))
