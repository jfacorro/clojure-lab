(ns lab.plugin.markdown.preview
  (:require [lab.core [plugin :refer [defplugin]]
                      [keymap :as km]]
            [lab.util :refer [timeout-channel]]
            [lab.model.protocols :as model]
            [lab.ui [core :as ui]
                    [templates :as tmplts]]
            [markdown.core :as md]))

(defn- view
  []
  (-> (tmplts/tab "html-preview")
    (ui/update-attr :header ui/update :label ui/attr :text "Html Preview")
    (ui/add [:scroll [:text-editor {:content-type "text/html"
                                    :line-highlight-color [0 0 0 0]
                                    :read-only true}]])))

(defn- update-html-from-doc
  "Updates the HTML preview control with the contents
  of the provided doc after it is parsed as markdown."
  [ui doc]
  (let [txt  (if doc (model/text @doc) "")
        html (md/md-to-html-string txt)]
    (ui/action
      (ui/update! ui [:#html-preview :text-editor]
                     ui/attr :text html))))

(defn- update-html!
  [{:keys [app source] :as e}]
  (let [ui (:ui @app)
        pos  (ui/caret-position source)
        html (md/md-to-html-string (model/text source))]
    (ui/action
      (ui/update! ui [:#html-preview :text-editor]
                     #(-> % 
                       (ui/attr :text html)
                       (ui/caret-position pos))))))

(defn- text-editor-init [editor]
  (let [hl-ch  (timeout-channel 250 #'update-html!)]
    (-> editor
      (ui/update-attr :stuff assoc ::listener hl-ch)
      (ui/listen :insert hl-ch)
      (ui/listen :delete hl-ch))))

(defn- text-editor-unload [editor]
  (let [hl-ch  (::listener (ui/stuff editor))]
    (-> editor
      (ui/update-attr :stuff dissoc ::listener)
      (ui/ignore :insert hl-ch)
      (ui/ignore :delete hl-ch))))

(defn- show-preview 
  [{:keys [app source] :as e}]
  (let [ui   (:ui @app)
        id   (ui/attr source :id)
        html (md/md-to-html-string (model/text source))
        tab  (-> (view) 
               ui/init
               (ui/update :text-editor ui/attr :text html))]
    (when-not (ui/find @ui :#html-preview)
      (ui/action
        (ui/update! ui :#right ui/add tab)
        (ui/update! ui (ui/id= id) text-editor-init)))))

(defn- switch-document-hook
  "Hook for #'lab.core/switch-document.
  Updates the preview of the markup document."
  [f app doc]
  (let [app (f app doc)]
    (update-html-from-doc (:ui app) doc)
    app))

(def ^:private hooks
  {#'lab.core/switch-document #'switch-document-hook})

(def ^:private keymaps
  [(km/keymap "Markdown"
     :local
     {:keystroke "ctrl p" :fn ::show-preview :name "Html Preview"})])

(defplugin "Markdown HTML Preview"
  :type    :local
  :hooks   hooks
  :keymaps keymaps)
