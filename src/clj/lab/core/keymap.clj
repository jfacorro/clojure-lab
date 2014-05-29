(ns lab.core.keymap
  (:refer-clojure :exclude [find remove])
  (:require [clojure.string :as str]))

(defn- ks->set [ks]
  (when ks
    (-> ks (str/split #" ") set)))

(defn- add-command
  [km {ks :keystroke :as cmd}]
  (assoc-in km [:bindings (ks->set ks)] cmd))

(defn commands
  "Returns a map with keymap names "
  [km]
  (loop [{:keys [parent bindings name] :as km} km
         cmds {}]
    (if km
      (recur parent (update-in cmds [name] into (vals bindings)))
      cmds)))

(defn keymap
  "Takes a name that should be a symbol or a keyword, a type (:global,
  :lang or :local) and any number of commands that are added as key-bindings."
  [name type & [lang & lang-cmds :as cmds]]
  (let [km {:name name :type type :bindings {}}]
    (if (= type :lang)
      (-> (reduce add-command km lang-cmds)
        (assoc :lang lang))
      (reduce add-command km cmds))))

(defn find
  "Given a keystroke looks for the corresponding commands in
  the supplied keymap and if not found in its parent."
  [km ks]
  (when km
    (if-let [cmd (get-in km [:bindings ks])]
      cmd
      (recur (:parent km) ks))))

(defn find-or
  "Takes a keymap and any number of keystrokes.
  Looks for the first keystroke that maps to a command 
  and returns this command."
  [km & kss]
  (->> (map (partial find km) kss)
    (drop-while nil?)
    first))

(defn append
  "Append a child keymap to an existinig one. If either one 
  is nil the other is returned."
  [parent child]
  (cond
    (nil? parent) child
    (nil? child)  parent
    :else         (assoc child :parent parent)))

(defn remove
  "Append a child keymap to an existinig one. If either one 
  is nil the other is returned."
  [top km-name]
  (loop [cur  top
         prev nil]
    (cond
      (nil? cur) top
      (= (:name cur) km-name)
        (if (:parent cur)
          (append (:parent cur) prev)
          (dissoc prev :parent))
      :else (recur (:parent cur) cur))))

(defmulti register-multi
  "Registers a keymap in x according to its type."
  (fn [x km] (:type km)))

(defmulti unregister-multi
  "Registers a keymap in x according to its type."
  (fn [x km] (:type km)))
