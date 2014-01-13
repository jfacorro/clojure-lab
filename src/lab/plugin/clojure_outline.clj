(ns lab.plugin.clojure-outline
  (:require [clojure.zip :as zip]
            [lab.ui.core :as ui]
            [lab.ui.templates :as tplts]
            [lab.model.document :as doc]
            [lab.core.lang :as lang]
            [lab.core.plugin :as plugin]
            [lab.core.keymap :as km]))

(defn definition? [node]
  (and (-> node zip/node :tag (= :list))
       (-> node zip/down zip/right zip/node 
         (as-> x
           (and (= (:tag x) :symbol)
                (.startsWith (-> x :content first) "def"))))))

(defn offset [node]
  (loop [node node
         offset 0]
    (if-not node
      offset
      (let [x  (zip/node node)]
        (recur (zip/prev node)
               (if (string? x)
                 (+ offset (.length x))
                 offset))))))

(defn def-from-node [node]
  {:offset (offset node)
   :name (-> node zip/down zip/right zip/right zip/right zip/down zip/node)})

(defn definitions [root]
  (let [node     (-> (zip/zipper map? :content nil root) 
                    zip/down)]
    (loop [node    node
           symbols []]
      (if-not node
        symbols
        (recur (zip/right node)
               (if (definition? node)
                 (conj symbols (def-from-node node))
                 symbols))))))

(defn- def-tree-node [def-info]
  [:tree-node {:leaf true 
               :item (:name def-info)}])

(defn- update-outline-tree [app doc]
  (let [ui         (:ui app)
        outline    (ui/find @ui :#outline-tree)]
    (when outline
      (if-not doc
        (when (seq (ui/children outline))
          (ui/action (ui/update! ui :#outline-tree ui/remove-all)))
        (let [parse-tree (lang/parse-tree @doc nil)
              def-infos  (definitions parse-tree)
              root       (into [:tree-node {:item (doc/name @doc)}]
                           (map def-tree-node def-infos))]
          (ui/action
            (ui/update! ui :#outline-tree 
              #(-> % ui/remove-all (ui/add root)))))))))

(defn- switch-document-hook
  "Hook for #'lab.core/switch-document:
  Updates the outline tree based on the document's lang 
and content"
  [f app doc]
  (future (update-outline-tree app doc))
  (f app doc))

(defn- outline-tree [app]
  (->
    (tplts/tab app {:label {:text "Outline"}})
    (ui/add [:scroll [:tree {:id "outline-tree"}]])))

(defn- create-outline-tree [app & _]
  (let [ui      (:ui @app)
        outline (ui/find @ui :#outline-tree)]
    (when-not outline
      (ui/update! ui :#right-controls ui/add (outline-tree app))
      (ui/update! ui (ui/parent "right-controls") ui/attr :divider-location 0.8))))

(def ^:private hooks
  {#'lab.core/switch-document #'switch-document-hook})

(def ^:private keymaps
  [(km/keymap (ns-name *ns*)
              :global
              {:category "View" :name "Outline" :fn #'create-outline-tree :keystroke "alt O"})])

(plugin/defplugin lab.plugin.clojure-outline
  :hooks hooks
  :keymaps keymaps)