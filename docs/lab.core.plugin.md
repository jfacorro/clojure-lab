# Plugin

  - Load a plugin.
  - Unload a plugin.
  - Keeps track of what plugins have been loaded.
  - Allow plugins to define their own dependencies (?)
  - Different levels of plugin (like keymaps) where the init! and unload! fns get:
    - :global -> the app atom
    - :lang   -> the lang atom
    - :doc    -> the current document atom