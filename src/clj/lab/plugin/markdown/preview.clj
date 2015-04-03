(ns lab.plugin.markdown.preview
  (:require [lab.core [plugin :refer [defplugin]]
                      [keymap :as km]
                      [main :as main]]
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
                     #(-> %
                       (ui/attr :text html)
                       (ui/caret-position 0))))))

(defn- update-html!
  [{:keys [app source] :as e}]
  (let [ui   (:ui @app)
        html (md/md-to-html-string (model/text source))
        pos  (ui/caret-position source)]
    (ui/action
      (ui/update! ui [:#html-preview :text-editor]
                     #(-> % 
                       (ui/attr :text html)
                       (ui/caret-position (min pos 
                                            (model/length %))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; hooks

(defn- switch-document-hook
  "Hook for #'lab.core/switch-document.
  Updates the preview of the markup document."
  [f app doc]
  (let [app (f app doc)]
    (future (update-html-from-doc (:ui app) doc))
    app))

(def ^:private hooks
  {#'lab.core/switch-document #'switch-document-hook})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; keymaps commands

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
        (ui/update! ui :#center-right
          (fn [x]
            (-> x
              (ui/update-attr :divider-location (constantly 0.5))
              (ui/update :#right ui/add tab))))))))

(def ^:private keymaps
  [(km/keymap "Markdown"
     :local
     {:keystroke "meta p" :fn ::show-preview :name "Html Preview"})])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; init!

(defn- text-editor-init [editor]
  (let [hl-ch  (timeout-channel 250 #'update-html!)]
    (-> editor
      (ui/update-attr :stuff assoc ::listener hl-ch)
      (ui/listen :insert hl-ch)
      (ui/listen :delete hl-ch))))

(defn- init! [app]
  (let [ui     (:ui @app)
        editor (main/current-text-editor @ui)
        id     (ui/attr editor :id)]
    (when editor
      (ui/update! ui (ui/id= id) text-editor-init))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; unload!

(defn- text-editor-unload [editor]
  (let [hl-ch  (::listener (ui/stuff editor))]
    (-> editor
      (ui/update-attr :stuff dissoc ::listener)
      (ui/ignore :insert hl-ch)
      (ui/ignore :delete hl-ch))))

(defn- unload! [app]
  (let [ui     (:ui @app)
        editor (main/current-text-editor @ui)
        id     (ui/attr editor :id)]
    (when editor
      (ui/update! ui (ui/id= id) text-editor-unload))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; plugin definition

(defplugin "Markdown HTML Preview"
  :type    :local
  :init!   #'init!
  :unload! #'unload!
  :hooks   hooks
  :keymaps keymaps)
