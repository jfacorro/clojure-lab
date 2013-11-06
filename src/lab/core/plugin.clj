(ns lab.core.plugin
  "Plugins are defined in a namespace where the following vars
should be defined:
  
init!      The initalizing function where the UI if any should be added.
hooks      A map where the keys are the target vars and the values are
           the functions that will be applied as hooks.
keymap     A keymap with global bindings that will be applied to the existing
           global keymap."
  (:require [robert.hooke :as hook]
            [lab.core.keymap :as km]
            [clojure.tools.logging :as log]))

(defn- add-hooks!
  "Add the defined hooks supplied and use the name
of the plugin as the hooks' key."
  [hooks hook-key]
  (doseq [[target-var f] hooks]
    (log/info hook-key "- Adding hook" f "to" target-var)
    (hook/add-hook target-var hook-key f)))

(defn register-keymap!
  "Uses the register multi-method. This is created as a function 
so that plugins can add hooks."
  [app keymap]
  (swap! app km/register-multi keymap))

(defn- register-keymaps!
  "Register all keymaps in the plugin."
  [app keymaps]
  (if keymaps
    (doseq [km keymaps]
      (register-keymap! app km))
    app))

(defn- load-plugin!
  "Receives the app atom and a symbol representing a plugin's
name(space) and requires the ns. The plugin's vars init!,
keymap and hooks are searched and processed accordingly if 
they exist."
  [app plugin-name]
  (require [plugin-name :reload true])
  (let [plugin-ns                      (the-ns plugin-name)
        {:keys [init! hooks keymaps] :as plugin} (->> (ns-resolve plugin-ns 'plugin) deref)]
    (assert plugin (str "Couldn't find a plugin definition in " plugin-name "."))
    (add-hooks! hooks plugin-name)
    (init! app)
    (register-keymaps! app keymaps)
    app))

(defn load-plugins!
  "Loads the plugins specified by calling the init! function
defined in their namespace."
  [app plugins]
  (reduce load-plugin! app plugins))

(defmacro defplugin
  "Defines a #'plugin var with the plugin's definition."
  [name & [docstr & opts :as options]]
  `(def ~'plugin
      ~(if (string? docstr) docstr (str "Plugin " name))
      (hash-map :name '~name ~@(if (string? docstr) opts options))))
