(ns lab.plugin.clojure-outline
  (:require [clojure.zip :as zip]
            [lab.core :as lab]
            [lab.core.lang :as lang]
            [lab.core.plugin :as plugin]
            [lab.core.keymap :as km]
            [lab.ui.core :as ui]
            [lab.ui.templates :as tplts]
            [lab.model.document :as doc]
            [lab.plugin.main-ui :as main-ui]))

(defn- node-tree-click
  [app e]
  (when (= 2 (:click-count e))
    (let [ui     (:ui @app)
          node   (:source e)
          info   (ui/attr node :info)
          editor (#'main-ui/current-text-editor @ui)
          id     (ui/attr editor :id)]
      (when id
        (ui/update! ui (ui/selector# id)
          #(-> %
            (ui/attr :caret-position (:offset info))
            ui/focus))))))

(defn- def-tree-node
  [app def-info]
  [:tree-node {:leaf true
               :item (:name def-info)
               :info def-info
               :on-click #'node-tree-click}])

(defn- update-outline-tree!
  "Updates the outline using the document provided
or the current document if non is specified."
  ([app]
    (update-outline-tree! app (lab/current-document app)))
  ([app doc]
    (let [ui         (:ui app)
          outline    (ui/find @ui :#outline-tree)]
      (when outline
        (if-not doc
          (ui/action (ui/update! ui :#outline-tree ui/remove-all))
          (let [lang       (doc/lang @doc)
                parse-tree (lang/parse-tree @doc nil)
                def-infos  (lang/definitions lang parse-tree)
                root       (into [:tree-node {:item (doc/name @doc)}]
                             (mapv (partial #'def-tree-node app) def-infos))]
            (ui/action
              (ui/update! ui :#outline-tree
                #(-> % ui/remove-all (ui/add root))))))))))

(defn- switch-document-hook
  "Hook for #'lab.core/switch-document:
  Updates the outline tree based on the document's lang
and content"
  [f app doc]
  (let [app (f app doc)]
    (future (#'update-outline-tree! app doc))
    app))

(defn- outline-tree [app]
  (->
    (tplts/tab app)
    (ui/update :label ui/attr :text "Outline")
    (ui/add [:scroll [:tree {:id "outline-tree"}]])))

(defn- create-outline-tree! [app & _]
  (let [ui      (:ui @app)
        outline (ui/find @ui :#outline-tree)]
    (when-not outline
      (ui/update! ui :#right ui/add (outline-tree app))
      (ui/update! ui (ui/parent "right") ui/attr :divider-location 0.7)
      (update-outline-tree! @app))))

(def ^:private hooks
  {#'lab.core/switch-document #'switch-document-hook})

(def ^:private keymaps
  [(km/keymap (ns-name *ns*)
              :global
              {:category "View" :name "Outline" :fn #'create-outline-tree! :keystroke "alt O"})])

(plugin/defplugin lab.plugin.clojure-outline
  :hooks hooks
  :keymaps keymaps)