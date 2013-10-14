(ns lab.core.keymap)

(defn add-command
  [km {ks :keystroke :as cmd}]
  (assoc-in km [:bindings ks] cmd))

(defn keymap
  "Takes a name that should be a symbol or a keyword, a type (:global
  or :local) and any number of commands that are added as key-bindings."
  [name type & cmds]
  (let [km  {:name name :type type :bindings {}}
        km  (reduce add-command km cmds)]
    km))

(defn find-command
  "Given a keystroke looks for the corresponding commands in
  the supplied keymap and if not found in its parent."
  [km ks]
  (when km
    (if-let [cmd (get-in km [:bindings ks])]
      cmd
      (recur (:parent km) ks))))

(defn append
  "Append a child keymap to an existing one."
  [child parent]
  (assoc child :parent parent))

(comment

{:name     'keymap-name
 :type     :global | :local
 :binding  {"ctrl O" {:category "File" :name "Open" :fn #'lab.app/open-document :keystroke "ctrl O"}}}

(keymap :catita :global
  {:category "File" :name "Open" :fn #'keymap :keystroke "ctrl O"}
  {:category "File" :name "Close" :fn #'keymap :keystroke "ctrl W"})

)