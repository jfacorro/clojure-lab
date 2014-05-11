# Plugins

The namespace `lab.core.plugin` implements the inner workings of the [loading](#loading) and [unloading](#unloading) of plugin, along with the macro `defplugin` which is used for their [definition](#definition) and creation. Most functionality is defined and implemented through plugins, which can be of two types: `:global` and `:local`.

`:global` plugins are meant for the ones that implement functionality that should be avaialble accross the whole application. These can be plugins that are either project/language agnostic (e.g. find & replace), or that should be globally available for a given project/language (e.g. the languages themselves, Clojure's nREPL).

`:local` plugins are more closely related to a document and its language, such as auto-complete and syntax-highlighting.

## Definition

A plugin is created through the use of `defplugin`, which defines a var named `plugin` with its definition in the current namespace. There are a number of fields associated with the definition of a plugin, except for the `name` all of the others are optional.

Usage:

    (defplugin 'plugin-name
      "Some docstring for the plugin var."
      :type    :global
      :keymaps [km1 km2 km3 ,,,]
      :hooks   {target-var1 hook-fn1 ,,,}
      :init!   init-fn!
      :unload! unload-fn!)

- `name` can be a symbol, a keyword or a string, all the rest of the fields are optional except for the `:type`:
- `:type` possible values are `:global` or `:local`.
- `:keymaps` vector that holds keymaps of different types which will be registered and unregisterd with the multimethods defined in `lab.core.keymap`.
- `:hooks` map with vars as keys and fns (or vars holding fns) as values, which will be used as a hook using the `robert-hooke` library.
- `:init!` function that takes a single argument which is the atom holding the whole app.
- `:unload!` function that takes a sinlge argument which is the atom holding the whole app.

## Loading

While `:global` plugins when loaded are registered in the application, `:local` plugins are registered in the application's current document. If a plugin is already loaded (which is detected by checking the registered plugins) then nothing is done.

The order of actions during the loading of a plugin is the following:

1. Register Plugin (in the app or current document).
2. Add Hooks (if any).
3. Invoke the `init!` function (if defined).
4. Register Keymaps (if any).

These are all side-effecting functions since at all times the application's atom is passed around. Adding hooks to a function using the `robert-hooke` library is also a side-effecting function.

## Unloading

The process of unloading a plugin basically follows the same steps as the loading but all modifications are undone:

1. Unregister Plugin (in the app or current document).
2. Remove Hooks (if any).
3. Invoke the `unload!` function (if defined).
4. Unregister Keymaps (if any).
