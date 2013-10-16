(ns lab.core.plugin
  "Plugins are defined in a namespace where the following vars
should be defined:
  
init!      The initalizing function where the UI if any should be added.
hooks      A map where the keys are the target vars and the values are
           the functions that will be applied as hooks.
keymap     A keymap with global bindings that will be applied to the existing
           global keymap."
  (:require [robert.hooke :as hook]
            [lab.core.keymap :as km]))

(defn- add-hooks
  "Add the defined hooks supplied and use the name
of the plugin as the hooks' key."
  [hooks hook-key]
  (doseq [[target-var f] hooks]
    (hook/add-hook target-var hook-key f)))

(defn- add-keymaps
  [app keymaps]
  (if keymaps
    (reduce km/register app keymaps)
    app))

(defn- load-plugin!
  "Receives the app atom and a symbol representing a plugin's
name(space) and requires the ns. The plugin's vars init!,
keymap and hooks are searched and processed accordingly if 
they exist."
  [app plugin-name]
  (require plugin-name)
  (println "Loading plugin " plugin-name)
  (let [plugin-ns            (the-ns plugin-name)
        resolve-var          (partial ns-resolve plugin-ns)
        [init! hooks keymaps] (map resolve-var '[init! hooks keymaps])]
    (assert (-> init! nil? not) (str "Couldn't find a function " (name 'init!) " in plugin " plugin-name "."))
    (add-hooks @hooks plugin-name)
    (swap! app add-keymaps @keymaps)
    (init! app)))

(defn load-plugins!
  "Loads the plugins specified by calling the init! function
defined in their namespace."
  [app plugins]
  (reduce load-plugin! app plugins))
