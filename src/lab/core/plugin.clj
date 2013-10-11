(ns lab.core.plugin)

(defn- load-plugin
  "Receives the app and a symbol representing a plugin's
  name(space). The namespace has to have an init  "
  [app plugin-name]
  (require plugin-name)
  (let [plugin-ns (the-ns plugin-name)
        init      (ns-resolve plugin-ns 'init)
        hooks     (ns-resolve plugin-ns 'hooks)
        keymaps   (ns-resolve fplugin-ns 'keymaps)]
    (assert (-> init nil? not) (str "Couldn't find an init function in " plugin-name "."))
    (init app)))

(defn load-plugins
  "Loads all files from the extension path specified in 
  the config map."
  [app plugin-type]
  (reduce load-plugin app (-> app :config plugin-type)))
