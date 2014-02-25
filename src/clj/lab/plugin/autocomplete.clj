(ns lab.plugin.autocomplete
  (:require [clojure.zip :as zip]
            [lab.util :refer [timeout-channel]]
            [lab.core [plugin :as plugin]
                      [keymap :as km]
                      [lang :as lang]
                      [trie :as trie]]
            [lab.model.protocols :as model]
            [lab.ui.core :as ui]
            [lab.ui.templates :as tplts]))

(defn- adjacent-string [loc dir]
  (lang/select-location (dir loc)
    dir
    lang/loc-string?))

(defn- select-autocomplete [loc app e]
  (let [node   (:source e)
        txt    (ui/attr node :item)
        editor (ui/attr node :stuff)
        [start end] (lang/limits loc)
        ws?     (lang/whitespace? loc)
        offset (if ws?
                 (ui/caret-position editor)
                 start)]
    (when (not ws?)
      (model/delete editor start end))
    (model/insert editor offset txt)
    (ui/focus editor)
    (ui/caret-position editor (+ offset (count txt)))))

(defn handle-keymap [km app e]
  (let [[x y](ui/key-stroke (dissoc e :source))
        cmd  (km/find-or km x y)]
    (when cmd
      (ui/consume e)
      (when (= :pressed (:event e))
        (ui/handle-event (:fn cmd) e)))))

(defn sym-tree-node [editor km sym-name]
  [:tree-node {:item sym-name
               :leaf true
               :stuff editor
               :listen [:key (partial handle-keymap km)]}])

(defn- matches-nodes [editor matches km]
  (-> [:tree-node {:item :root}]
    (into (map (partial sym-tree-node editor km) matches))))

(defn popup-menu [editor loc matches]
  (let [location (ui/caret-location editor)
        km    (km/keymap :autocomplete :local
                {:fn (partial select-autocomplete loc) :keystroke "enter"})
        popup    (-> [:pop-up-menu {:location location
                                    :source   editor
                                    :border   :none}
                       [:scroll {:size [250 100]
                                 :border :none}
                         [:tree {:hide-root true}
                           (matches-nodes editor matches km)]]]
                  ui/init)]
    (ui/attr popup :visible true)
    (ui/update popup :tree ui/focus)))

(defn- autocomplete [app {:keys [event description modifiers] :as e}]
  (when (and (= :pressed event) (= description :space) (modifiers :ctrl))
  (let [editor (:source e)
        pos    (ui/caret-position editor)
        doc    (ui/attr editor :doc)
        tree   (lang/code-zip (lang/parse-tree @doc))
        [loc i](lang/location tree pos)
        loc    (if (and (lang/whitespace? loc) (= pos i))
                 (adjacent-string loc zip/prev)
                 loc)
        tag    (lang/location-tag loc)
        ploc  (zip/up loc)
        symbols(->> (lang/search tree #(and (= :symbol (-> % zip/node :tag)) (not= ploc %)))
                  (map #(-> % zip/down zip/node))
                  set
                  sort)]
    (popup-menu editor loc
      (if (= tag :symbol)
        (-> symbols trie/trie (trie/prefix-matches (zip/node loc)))
        symbols)))))

(defn- text-editor-hook [f doc]
  (let [editor (f doc)]
    (-> editor
      (ui/listen :key #'autocomplete))))

(def ^:private hooks
  {#'lab.ui.templates/text-editor #'text-editor-hook})

(plugin/defplugin lab.plugin.autocomplete
  :hooks hooks)
