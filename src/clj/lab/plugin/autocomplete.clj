(ns lab.plugin.autocomplete
  (:require [clojure.zip :as zip]
            [lab.core [plugin :as plugin]
                      [keymap :as km]
                      [lang :as lang]
                      [trie :as trie]]
            [lab.model.protocols :as model]
            [lab.ui.core :as ui]
            [lab.ui.templates :as tplts]))

(defn- adjacent-string
  "Returns the first location that's adjacent
to the current in the direction specified."
  [loc dir]
  (lang/select-location (dir loc)
                        dir
                        lang/loc-string?))

(defn- select-autocomplete [e]
  (let [node   (:source e)
        txt    (ui/attr node :item)
        {:keys [editor loc popup]} (ui/stuff node)
        [start end] (lang/limits loc)
        ws?     (lang/whitespace? loc)
        offset (if ws?
                 (ui/caret-position editor)
                 start)]
    (when (not ws?)
      (model/delete editor start end))
    (ui/attr popup :visible false)
    (-> editor
      (model/insert offset txt)
      (ui/caret-position (+ offset (count txt)))
      ui/focus)))

(defn handle-keymap [km e]
  (let [[x y](ui/key-stroke (dissoc e :source))
        cmd  (km/find-or km x y)]
    (when cmd
      (ui/consume e)
      (when (= :pressed (:event e))
        (ui/handle-event (:fn cmd) e)))))

(defn sym-tree-node [stuff km sym-name]
  [:tree-node {:item sym-name
               :leaf true
               :stuff stuff
               :listen [:key (partial handle-keymap km)]}])

(defn- matches-nodes [stuff matches km]
  (-> [:tree-node {:item :root}]
    (into (map (partial sym-tree-node stuff km) matches))))

(defn popup-menu [editor loc matches]
  (let [location (ui/caret-location editor)
        km    (km/keymap :autocomplete :local
                {:fn ::select-autocomplete :keystroke "enter"})
        popup (ui/init
                [:pop-up-menu {:location location
                               :source   editor
                               :border   :none}
                 [:scroll {:size [250 100]
                           :border :none}
                  [:tree {:hide-root true}]]])
        stuff {:editor editor :loc loc :popup popup}
        root  (matches-nodes stuff matches km)]
    (-> popup
      (ui/update :tree ui/add root)
      (ui/attr :visible true)
      (ui/update :tree ui/focus))))

(defn- symbols-in-scope
  "Gets all the siblings of the current location and their
inner and outer symbols in scope."
  [loc]
  (->> (if (zip/up loc)
         [(zip/node loc)]
         (zip/children loc))
       (mapcat #(mapcat (fn [x] (-> % meta :scope x))
                        (if (zip/up loc)
                          [:in :out]
                          [:out])))
       (map (comp first :content))))

(defn- collect-symbols-in-scope [loc]
  (loop [loc  loc
         symbols (into #{} (symbols-in-scope loc))]
    (if-not loc
      symbols
      (recur (zip/up loc) (into symbols (symbols-in-scope loc))))))

(defn- autocomplete [e]
  (let [editor  (:source e)
        pos     (ui/caret-position editor)
        doc     (ui/attr editor :doc)
        root    (lang/code-zip (lang/parse-tree @doc))
        [loc i] (lang/location root pos)
        loc     (if (and (lang/whitespace? loc) (= pos i))
                  (adjacent-string loc zip/prev)
                  loc)
        symbols (->> (collect-symbols-in-scope loc) set sort)]
    (popup-menu editor loc
      (if (= (lang/location-tag loc) :symbol)
        (-> symbols trie/trie (trie/prefix-matches (zip/node loc)))
        symbols))))

(def ^:private keymaps
  [(km/keymap 'lab.plugin.autocomplete
      :local
      {:fn ::autocomplete :keystroke "ctrl space"})])

(plugin/defplugin lab.plugin.autocomplete
  :type    :local
  :keymaps keymaps)
