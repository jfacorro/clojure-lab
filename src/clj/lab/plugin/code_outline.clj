(ns lab.plugin.code-outline
  (:require [clojure.zip :as zip]
            [lab.core :as lab]
            [lab.core.lang :as lang]
            [lab.core.plugin :as plugin]
            [lab.core.keymap :as km]
            [lab.util :as util]
            [lab.ui.core :as ui]
            [lab.ui.templates :as tplts]
            [lab.model.document :as doc]
            [lab.plugin.main-ui :refer [current-text-editor]]))

(defn- go-to-definition [ui line-number]
  (let [editor (current-text-editor @ui)
        id     (ui/attr editor :id)]
    (when id
      (ui/update! ui (ui/selector# id)
        #(-> %
          (ui/caret-position line-number)
          ui/focus)))))

(defn- go-to-definition-enter
  "Handles the enter press in a tree node positioning the
caret in the definition associated with the tree node."
  [{:keys [app source description event] :as e}]
  (when (and (= :pressed event) (= :enter description))
    (let [ui     (:ui @app)
          info   (ui/attr source :info)]
      (go-to-definition ui (:offset info)))))

(defn- go-to-definition-click
  "Handles the click in a tree node positioning the
caret in the definition associated with the tree node."
  [e]
  (when (= 2 (:click-count e))
    (let [ui     (-> e :app deref :ui)
          node   (:source e)
          info   (ui/attr node :info)]
      (go-to-definition ui (:offset info)))))

(defn- def->tree-node
  [app def-info]
  [:tree-node {:leaf true
               :item (:name def-info)
               :info def-info
               :listen [:key ::go-to-definition-enter
                        :click ::go-to-definition-click]}])

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
          (let [lang        (doc/lang @doc)
                definitions (:definitions lang)
                parse-tree  (lang/parse-tree @doc nil)
                defs        (and definitions (definitions parse-tree))
                root        (into [:tree-node {:item (doc/name @doc)}]
                              (map (partial #'def->tree-node app) defs))]
            (ui/action
              (ui/update! ui :#outline-tree
                #(-> % ui/remove-all (ui/add root))))))))))

(defn- outline-tree
  "Creates a new tab that contains a tree with id :#outline-tree."
  [app]
  (-> (tplts/tab "outline-tab")
    (ui/update :tab
               ui/update-attr :header
               ui/update :label ui/attr :text "Outline")
    (ui/add [:scroll [:tree {:id "outline-tree"}]])
    (ui/apply-stylesheet (:styles @app))))

(defn- create-outline-tree! [e]
  (let [app     (:app e)
        ui      (:ui @app)
        outline (ui/find @ui :#outline-tree)]
    (if-not outline
      (let [split (ui/find @ui (ui/parent "right"))]
        (ui/update! ui :#right ui/add (outline-tree app))
        (when-not (ui/attr split :divider-location-right)
          (ui/update! ui (ui/parent "right") ui/attr :divider-location-right 150))
        (update-outline-tree! @app))
      (when-let [tab (ui/find @ui :#outline-tab)]
        (ui/update! ui :#right ui/remove tab)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Hooks

(defn- switch-document-hook
  "Hook for #'lab.core/switch-document:
  Updates the outline tree based on the document's lang
and content"
  [f app doc]
  (let [app (f app doc)]
    (future (#'update-outline-tree! app doc))
    app))

(defn- text-editor-hook [f doc]
  (let [editor (f doc)
        g      (fn [e] (#'update-outline-tree! @(:app e) doc))
        ch     (util/timeout-channel 500 g)]
    (-> editor
      (ui/listen :insert ch)
      (ui/listen :delete ch))))

(def ^:private hooks
  {#'lab.core/switch-document #'switch-document-hook
   #'lab.ui.templates/text-editor #'text-editor-hook})

(def ^:private keymaps
  [(km/keymap (ns-name *ns*)
              :global
              {:category "View" :name "Outline" :fn #'create-outline-tree! :keystroke "alt O"})])

(plugin/defplugin lab.plugin.code-outline
  :type  :global
  :hooks hooks
  :keymaps keymaps)
