(ns lab.plugin.editor.autocomplete
  (:require [clojure.zip :as zip]
            [lab.core [plugin :as plugin]
                      [keymap :as km]
                      [lang :as lang]
                      [trie :as trie]]
            [lab.model [protocols :as model]
                       [document :as doc]]
            [lab.ui [core :as ui]
                    [templates :as tplts]]
            [lab.plugin.clojure.nrepl :as nrepl]))

(defn token-location-at-caret
  "Takes an editor and finds the token immediately after
the current caret position."
  [editor]
  (let [root    (-> @(ui/attr editor :doc)
                    lang/parse-tree
                    lang/code-zip)
        pos     (ui/caret-position editor)
        [loc i] (lang/location root pos)
        tag     (lang/location-tag loc)
        prev    (or (and (= :symbol tag)       ; at a symbol
                         (zip/up loc))
                    (zip/left loc)             ; at a closing delimiter
                    (-> loc zip/up zip/left))  ; at the end of a whitespace
        tag     (lang/location-tag prev)]
    (when (and prev
               (or (= :symbol tag)
                   (and (not= :symbol tag) (= pos i))))
      prev)))

(defn- select-autocomplete
  "Takes an event whose source is a node from the autocompletion
  tree list. Identifies the selected option in the autocomplete
  popup menu and replaces the token at the caret position with 
  the selection."
  [{:keys [source] :as e}]
  (let [txt    (ui/attr source :item)
        {:keys [editor popup]}
               (ui/stuff source)
        loc    (token-location-at-caret editor)
        [start end]
               (lang/limits loc)
        ws?    (lang/whitespace? loc)
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

(defn- matches-nodes [editor popup matches]
  (let [km    (km/keymap "Autocomplete" :local
                         {:fn ::select-autocomplete :keystroke "enter"})
        stuff {:editor editor :popup popup}]
    (-> [:tree-node {:item :root}]
      (into (map (fn [sym-name]
                   [:tree-node {:item sym-name
                                :leaf true
                                :stuff stuff
                                :listen [:key km]}])
                 matches)))))

(defn popup-menu
  [editor matches]
  (let [location (ui/caret-location editor)
        popup (ui/init
                [:pop-up-menu {:location location
                               :source   editor
                               :border   :none}
                 [:scroll {:size [250 100]}
                  [:tree {:hide-root true}]]])
        root  (matches-nodes editor popup matches)]
    (-> popup
      (ui/update :tree ui/add root)
      (ui/attr :visible true)
      (ui/update :tree ui/focus))))

(defn- completion-tokens
  "Gets the auto-completion functions from the current document's
lang and runs them accumulating theirs results in a set.
Returns nil if there's no token in the current caret position."
  [{:keys [app source] :as e}]
  (let [lang  (:lang @(ui/attr source :doc))
        fns   (:autocomplete lang)]
    (when-let [loc (token-location-at-caret source)]
      (-> (reduce into #{} (map #(% e) fns))
          trie/trie
          (trie/prefix-matches (-> loc zip/down zip/node))))))

;;;;;;;;;;;;;;;;;;;;;;;;
;; Plugin definition

(defn- autocomplete
  [{:keys [app source] :as e}]
  (when-let [symbols (completion-tokens e)]
    (-> (popup-menu source (sort symbols))
      (ui/apply-stylesheet (:styles @app)))))

(def ^:private keymaps
  [(km/keymap "Autocomplete"
     :local
     {:fn ::autocomplete :keystroke "meta space" :name "Autocomplete"})])

(plugin/defplugin lab.plugin.editor.autocomplete
  :type    :local
  :keymaps keymaps)
