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
  "Add the defined hooks and use the name
of the plugin as the hooks' key."
  [hooks hook-key]
  (doseq [[target-var f] hooks]
    (log/info hook-key "- Adding hook" f "to" target-var)
    (hook/add-hook target-var hook-key f)))

(defn- remove-hooks!
  "Remove the defined hooks using the name
of the plugin as the hooks' key."
  [hooks hook-key]
  (doseq [[target-var f] hooks]
    (log/info hook-key "- Removing hook" f "in" target-var)
    (hook/remove-hook target-var hook-key)))

(defn register-keymap!
  "Uses the register multi-method. This is created as a function 
so that plugins can add hooks."
  [app keymap]
  (swap! app km/register-multi keymap))

(defn- register-keymaps!
  "Register all keymaps in the plugin."
  [app keymaps]
  (doseq [km keymaps]
    (register-keymap! app km)))

(defn unregister-keymap!
  "Uses the unregister multi-method."
  [app keymap]
  (swap! app km/unregister-multi keymap))

(defn- unregister-keymaps!
  "Unregister all keymaps in the plugin."
  [app keymaps]
  (doseq [km keymaps]
    (unregister-keymap! app km)))

(defn load-plugin!
  "Receives the app atom and a symbol representing a plugin's
name(space) and requires the ns. The plugin's vars init!,
keymap and hooks are searched and processed accordingly if 
they exist."
  [app plugin-name & [reload]]
  (require [plugin-name :reload reload])
  (let [plugin-ns                      (the-ns plugin-name)
        {:keys [init! hooks keymaps] :as plugin} (->> (ns-resolve plugin-ns 'plugin) deref)]
    (assert plugin (str "Couldn't find a plugin definition in " plugin-name "."))
    (when hooks
      (add-hooks! hooks plugin-name))
    (when init!
      (init! app))
    (when keymaps
      (register-keymaps! app keymaps))
    app))

(defn load-plugins!
  "Loads the plugins specified by calling the init! function
defined in their namespace."
  [app plugins]
  (reduce load-plugin! app plugins))

(defn unload-plugin!
  "Receives the app atom and a symbol representing a plugin's
name(space). Unloads all hooks, removes all keymaps and calls
the unload! function."
  [app plugin-name & [reload]]
  (let [plugin-ns (the-ns plugin-name)
        {:keys [unload! hooks keymaps] :as plugin}
                  (->> (ns-resolve plugin-ns 'plugin) deref)]
    (assert plugin (str "Couldn't find a plugin definition in " plugin-name "."))
    (when hooks
      (remove-hooks! hooks plugin-name))
    (when unload!
      (unload! app))
    (when keymaps
      (unregister-keymaps! app keymaps))
    app))

(defmacro defplugin
  "Defines a `#'plugin` var with the plugin's definition. All of 
the following values are optional when defining the plugin.

Usage:

    (defplugin 'plugin.name
      :keymaps [km1 km2 km3 ,,,]
      :hooks   {target-var1 hook-fn1 ,,,}
      :init!   init-fn!
      :unload! unload-fn!)

Where:
  - `:keymaps` is a vector that can hold keymaps of different type
    which will be registered and unregisterd with the multimethods
    defined in `lab.core.keymap`.
  - `:hooks` is a map with vars as keys and fns (or vars holding fns)
    as values, which will be used as a hook using the robert-hooke lib.
  - `:init!` is a fn that will be called when loading the plugin, after 
    the keymaps are registered and receives a single argument which is 
    the atom holding the whole app.
  - `:unload!` is a fn that will be called when unloading the plugin, after 
    the keymaps have been unregistered and before the hooks are removed."
  [name & [docstr & opts :as options]]
  `(def ~'plugin
      ~(if (string? docstr) docstr (str "Plugin " name))
      (hash-map :name '~name ~@(if (string? docstr) opts options))))
