(ns lab.plugin.syntax-highlighting
  (:require [clojure.core.async :as async]
            [lab.util :refer [timeout-channel]]
            [lab.ui.core :as ui]
            [lab.model.document :as doc]
            [lab.core [plugin :as plugin]
                      [lang :as lang]]))

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

(defn- text-editor-change! [app e]
  (if-not (= :change (:type e))
    (highlight! (:source e) true)))

(defn- text-editor-hook [f doc]
  (let [editor (f doc)
        hl-ch  (timeout-channel 100 #'text-editor-change!)]
    (-> editor
      highlight!
      (ui/listen :insert hl-ch)
      (ui/listen :delete hl-ch))))

(def ^:private hooks
  {#'lab.ui.templates/text-editor #'text-editor-hook})

(plugin/defplugin lab.plugin.syntax-highlighting
  :hooks hooks)

