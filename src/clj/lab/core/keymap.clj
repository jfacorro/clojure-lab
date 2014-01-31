(ns lab.core.keymap
  (:refer-clojure :exclude [find])
  (:require [clojure.string :as str]))

(defn ks->set [ks]
  (-> ks
    (str/split #" ")
    set))

(defn add-command
  [km {ks :keystroke :as cmd}]
  (assoc-in km [:bindings (ks->set ks)] cmd))

(defn keymap
  "Takes a name that should be a symbol or a keyword, a type (:global,
:lang or :local) and any number of commands that are added as key-bindings."
  [name type & [lang & lang-cmds :as cmds]]
  (let [km  {:name name :type type :bindings {}}
        km  (if (= type :lang)
              (-> (reduce add-command km lang-cmds)
                (assoc :lang lang))
              (reduce add-command km cmds))]
    km))

(defn find
  "Given a keystroke looks for the corresponding commands in
the supplied keymap and if not found in its parent."
  [km ks]
  (when km
    (if-let [cmd (get-in km [:bindings ks])]
      cmd
      (recur (:parent km) ks))))

(defn append
  "Append a child keymap to an existinig one. If either one 
is nil the other is returned."
  [parent child]
  (cond
    (nil? parent) child
    (nil? child)  parent
    :else         (assoc child :parent parent)))

(defmulti register-multi
  "Registers a keymap in the app according to its type."
  (fn [app km] (:type km)))

(comment

{:name     'keymap-name
 :type     :global ; or :lang or :local
 :binding  {"ctrl O" {:category "File" :name "Open" :fn #'lab.core/open-document :keystroke "ctrl O"}}}

(keymap :catita :global
  {:category "File" :name "Open" :fn #'keymap :keystroke "ctrl O"}
  {:category "File" :name "Close" :fn #'keymap :keystroke "ctrl W"})

)