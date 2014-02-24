(ns lab.plugin.autocomplete
  (:require [clojure.zip :as zip]
            [lab.util :refer [timeout-channel]]
            [lab.core [plugin :as plugin]
                      [keymap :as km]
                      [lang :as lang]
                      [trie :as trie]]
            [lab.ui.core :as ui]
            [lab.ui.templates :as tplts]))

(defn- adjacent-string-loc [loc dir]
  (lang/select-location (dir loc)
    dir
    #(not (lang/whitespace? %))))

(defn sym-tree-node [sym-name]
  [:tree-node {:item sym-name
               :leaf true}])

(defn- matches-nodes [matches]
  (-> [:tree-node {:item :root}]
    (into (map sym-tree-node matches))))

(defn popup-menu [app e matches]
  (let [editor (:source e)
        location (ui/caret-location editor)
        popup  (-> [:pop-up-menu {:location location
                                  :source   editor
                                  :border   :none}
                     [:scroll {:size [250 100]
                               :border :none}
                       [:tree {:hide-root true}
                         (matches-nodes matches)]]]
                ui/init)]
    (ui/attr popup :visible true)
    (ui/update popup :tree ui/focus)))

(defn- autocomplete [app {:keys [event description modifiers] :as e}]
  (when (and (= :pressed event) (= description :space) (modifiers :ctrl))
  (let [editor (:source e)
        pos    (ui/caret-position editor)
        doc    (ui/attr editor :doc)
        tree   (lang/code-zip (lang/parse-tree @doc))
        symbols(->> (lang/search tree #(= :symbol (-> % zip/node :tag)))
                  (map #(-> % zip/down zip/node))
                  set
                  sort)
        [loc i](lang/location tree pos)
        loc    (if (lang/whitespace? loc)
                 (adjacent-string-loc loc zip/prev)
                 loc)
        tag    (lang/location-tag loc)]
    (if (= tag :symbol)
      (popup-menu app e (-> symbols trie/trie (trie/prefix-matches (zip/node loc))))
      (popup-menu app e symbols)))))

(defn- text-editor-hook [f doc]
  (let [editor (f doc)]
    (-> editor
      (ui/listen :key #'autocomplete))))

(def ^:private hooks
  {#'lab.ui.templates/text-editor #'text-editor-hook})

(plugin/defplugin lab.plugin.autocomplete
  :hooks hooks)
