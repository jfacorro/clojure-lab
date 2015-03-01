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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Hooks

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Keymaps

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Register plugin

(defmulti register-plugin!
  "Registers the plugin when it is loaded. Returns
nil if the plugin was already registered and not
nil otherwise."
  (fn [_ plugin] (:type plugin)))

(defmulti unregister-plugin!
  "Removes the plugin if it was registered. Returns
nil if the plugin was not registered and not nil otherwise."
  (fn [_ plugin] (:type plugin)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Load / Unload

(defn load-plugin!
  "Receives the app atom and a symbol representing a plugin's
name(space) and requires the ns. The plugin's vars init!,
keymap and hooks are searched and processed accordingly if 
they exist."
  [app plugin-name & [reload]]
  (require [plugin-name :reload reload])
  (let [plugin-ns  (the-ns plugin-name)
        plugin     (ns-resolve plugin-ns 'plugin)
        _          (assert plugin (str "Couldn't find a plugin definition in " plugin-name "."))
        {:keys [init! hooks keymaps] :as plugin}
                   @plugin]
    (log/info "Loaded plugin " plugin-name)
    (when (register-plugin! app plugin)
      (log/info "Registered plugin " plugin-name)
      (when hooks
        (add-hooks! hooks plugin-name))
      (when init!
        (init! app))
      (when keymaps
        (register-keymaps! app keymaps)))))

(defn unload-plugin!
  "Receives the app atom and a symbol representing a plugin's
name(space). Unloads all hooks, removes all keymaps and calls
the unload! function."
  [app plugin-name & [reload]]
  (let [plugin-ns (the-ns plugin-name)
        {:keys [unload! hooks keymaps] :as plugin}
                  (->> (ns-resolve plugin-ns 'plugin) deref)]
    (assert plugin (str "Couldn't find a plugin definition in " plugin-name "."))
    (when (unregister-plugin! app plugin)
      (when hooks
        (remove-hooks! hooks plugin-name))
      (when unload!
        (unload! app))
      (when keymaps
        (unregister-keymaps! app keymaps)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Definition

(defmacro defplugin
  "Defines a `#'plugin` var with the plugin's definition. All of 
the following values are optional when defining the plugin, except for
the name.

Usage:

    (defplugin 'plugin.name
      \"Some docstring for the plugin var.\"
      :type    :global
      :keymaps [km1 km2 km3 ,,,]
      :hooks   {target-var1 hook-fn1 ,,,}
      :init!   init-fn!
      :unload! unload-fn!)

`name` can be a symbol, a keyword or a string, all the rest of the
fields are optional except for the `:type`:
  - `:type` should be one of `:global` or `:local`, this determines where
    the plugin is registered as loaded.
  - `:keymaps` vector that holds keymaps of different types
    which will be registered and unregisterd with the multimethods
    defined in `lab.core.keymap`.
  - `:hooks` map with vars as keys and fns (or vars holding fns)
    as values, which will be used as a hook using the robert-hooke lib.
  - `:init!` fn that takes a single argument which is the atom holding
     the whole app.
  - `:unload!` fn that takes a single argument which is the atom holding
     the whole app."
  [name & [docstr & opts :as options]]
  `(def ~'plugin
      ~(if (string? docstr) docstr (str "Plugin " name))
      (hash-map :name '~name ~@(if (string? docstr) opts options))))
