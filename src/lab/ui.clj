(ns lab.ui
  "DSL to abstract the UIcomponents with Clojure data structures."
  (:require [lab.ui.core :as ui :reload true]
            [lab.ui.select :as ui.sel :reload true]
            [lab.ui.tree :as tree]
            [lab.ui.protocols :as uip]
            [lab.ui.swing :reload true]))

(defn- new-text-editor [file]
  (ui/text-editor :text        (slurp file)
                  :border      :none
                  :background  0x666666
                  :foreground  0xFFFFFF
                  :caret-color 0xFFFFFF
                  :font        [:name "Consolas" :size 14]
                  :on-insert   #(do (println %) (println "insert"))
                  :on-delete   #(do (println %) (println "delete"))
                  :on-change   #(do % (println "change"))))

(defn close-tab [ui id & _]
  (let [tab  (ui/find @ui (str "#" id))]
    (swap! ui ui/update :#documents uip/remove tab)))

(defn- new-tab [ui item]
  (let [id    (ui/genid)
        path  (.getCanonicalPath ^java.io.File item)
        text  (new-text-editor item)
        close (partial #'close-tab ui id)]
    (ui/tab :-id  id
            :-tool-tip path
            :-header   (ui/panel :opaque false
                                 :content [(ui/label :text (str item))
                                           (ui/button :icon "close-tab.png"
                                                      :border :none
                                                      :on-click close)])
            :border  :none
            :content text)))

(defn open-file [ui evt]
  (let [^java.io.File file (-> evt uip/source uip/get-selected)]
    (when (-> file .isDirectory not)
      (swap! ui ui/update :tabs uip/add (new-tab ui file)))))

(defn build-main [{ui :ui name :name :as app}]
  (ui/window :-id     "main"
             :title   name
             :size    [700 500]
             :icons   ["icon-16.png" "icon-32.png" "icon-64.png"]
             :menu    (ui/menu-bar)
             :content (ui/split :orientation :horizontal
                                :border      :none
                                :content [(ui/tree :-id "file-tree" 
                                                   :on-dbl-click (partial #'open-file ui)
                                                   :root (tree/load-dir ".."))
                                          (ui/tabs :-id "documents" :border :none)])))

;; Menu

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

(defn add-menu-option
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

;; Init

(defn init [app]
  (let [ui  (atom nil)
        app (assoc app :ui ui)]
    (reset! ui (-> app build-main ui/init))
    #_(do
      (swap! ui add-menu-option {:menu "File" :name "New" :action #(println "New" (class %2)) :key-stroke "ctrl N"})
      (swap! ui add-menu-option {:menu "File" :name "Open" :action #(println "Open" (class %2)) :key-stroke "ctrl O"})
      (swap! ui add-menu-option {:menu "File -> Project" :name "New" :action #(println "New Project" (class %2))})
      (swap! ui add-menu-option {:menu "File" :separator true})
      (swap! ui add-menu-option {:menu "File" :name "Exit" :action #(do %& (System/exit 0))})
      (swap! ui add-menu-option {:menu "Edit" :name "Copy" :action #(println "Exit" (class %2))}))
    app))

#_(do
  (-> {:name "Clojure Lab - UI dummy"}
    init
    :ui
    (swap! ui/init)
    uip/show)
  nil)