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
                                    :read-only true}]])))

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

(def ^:private keymaps
  [(km/keymap ::keymap
     :local
     {:keystroke "ctrl p" :fn ::show-preview})])

(defplugin "Markdown HTML Preview"
  :type  :global
  :keymaps keymaps)
