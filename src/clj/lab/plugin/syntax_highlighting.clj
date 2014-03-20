(ns lab.plugin.syntax-highlighting
  (:require [clojure.core.async :as async]
            [lab.util :refer [timeout-channel]]
            [lab.ui.core :as ui]
            [lab.model.document :as doc]
            [lab.core [plugin :as plugin]
                      [lang :as lang]
                      [main :as main]]))

(defn highlight!
  "Takes the editor component and an optional argument
that indicates if the highlight should be incremental
or not.

If it's incremental only the highlight modified since the
last parse tree generation are update, otherwise all tokens
are applied their highlight."
  [editor & [incremental]]
  (let [doc         (ui/attr editor :doc)
        node-group  (and incremental (gensym "group-"))
        lang        (doc/lang @doc)
        styles      (:styles lang)
        old-text    (doc/text editor)
        parse-tree  (lang/parse-tree @doc node-group)
        tokens      (lang/tokens parse-tree node-group)
        ;; If there are no tokens for this group then take the group from the root node.
        tokens      (if (empty? tokens)
                      (lang/tokens parse-tree (lang/node-group parse-tree))
                      tokens)]
    (ui/action
      ;; Before applying the styles check that the
      ;; text is still the same, otherwise some tokens
      ;; get messed up.
      (when (= (doc/text editor) old-text)
        (ui/apply-style editor tokens styles))))
  editor)

(defn- text-editor-change! [e]
  (if-not (= :change (:type e))
    (highlight! (:source e) true)))

(defn- text-editor-init [editor]
  (let [hl-ch  (timeout-channel 250 #'text-editor-change!)]
    (-> editor
      highlight!
      (ui/update-attr :stuff assoc ::listener hl-ch)
      (ui/listen :insert hl-ch)
      (ui/listen :delete hl-ch))))

(defn- text-editor-unload [editor]
  (let [hl-ch  (::listener (ui/stuff editor))]
    (-> editor
      (ui/update-attr :stuff dissoc ::listener)
      (ui/ignore :insert hl-ch)
      (ui/ignore :delete hl-ch))))

(defn init! [app]
  (let [ui     (:ui @app)
        editor (main/current-text-editor @ui)
        id     (ui/attr editor :id)]
    (ui/update! ui (ui/id= id) text-editor-init)))

(defn unload! [app]
  (let [ui (:ui @app)
        id (ui/attr (main/current-text-editor @ui) :id)]
    (ui/update! ui (ui/id= id) text-editor-unload)))

(plugin/defplugin lab.plugin.syntax-highlighting
  :type    :local
  :init!   init!
  :unload! unload!)
