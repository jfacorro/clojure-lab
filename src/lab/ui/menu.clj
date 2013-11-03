(ns lab.ui.menu
  (:require [lab.util :as util]
            [lab.ui [core :as ui]
                    [select :as ui.sel]]))

(defn- menu-path
  "Deconstructs a menu path from a string with a '->' separator."
  [^String s]
  (when s
    (->> (.split s ">") seq (map clojure.string/trim))))

(defn- create-menu-path
  "Searches the menu-bar children using the selector. If the
  menu defined is not found it is created, otherwise the menu-bar 
  is returned unchanged."
  [menu-bar selector]
  (if (ui/find menu-bar selector)
    menu-bar
    (let [text     (-> selector last meta :value) ; The meta from the last selector's predicate has the name of the menu.
          menu     [:menu {:text text}]
          selector (or (butlast selector) [])]
      (ui/update menu-bar selector ui/add menu))))

(defn add-option
  "Takes a menu option and add it to the ui menu bar.
  The menu option map must have the following keys:
    :category   path in the menu.
    :name       name of the option.
    :fn     var to a function with args [ui evt & args].
    :separator  true if the option should be followed by a separator."
  [app ui {:keys [category name fn separator keystroke] :as option}]
  (assert (instance? clojure.lang.IRef app) "app should be a reference of some type.")
  (let [menu-bar  (ui/get-attr ui :menu)
                  ;; Explode the menu path and build a selector.
        selector  (map (partial ui.sel/attr= :text) (menu-path category))
                  ;; Build selectors for each of the menu path levels.
        selectors (map #(->> selector (take %1) vec) (range 1 (-> selector count inc)))
        item      (if separator
                    [:menu-separator]
                    [:menu-item {:text name :on-click (util/event-handler fn app) :keystroke keystroke}])
        menu-bar  (reduce create-menu-path menu-bar selectors)
        menu-bar  (ui/update menu-bar selector ui/add item)]
     (ui/set-attr ui :menu menu-bar)))
