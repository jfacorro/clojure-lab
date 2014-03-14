# The Core of the App

## Document management
  - Loading a document.
  - Keeps track of the current document that's being worked on.
  - Saving the changes to a modified document.
  - Switching documents.
  - Maps a document to a language (see Lang mgmt).

## Language management
  - Assigns a language to each opened document.
  - A language is assigned based on rules.
    - The most basic one is the extension of the file.
    - Arbitrary rules can be added.
  - There has to be a default language (plain document).
  - Properties
    - name: descriptive name.
    - options: Parsley parser options.
    - grammar: special forms, literals (numbers, strings, etc.).
    - lang?: Predicate function that receives a doc and returns true if the language applies to the doc.

## Plugin management
  - Load a plugin.
  - Unload a plugin.
  - Keeps track of what plugins have been loaded.
  - Allow plugins to define their own dependencies (?)
  - Different levels of plugin (like keymaps) where the init! and unload! fns get:
    - :global -> the app atom
    - :lang   -> the lang atom
    - :doc    -> the current document atom

## Keymap management
  - Register keymaps at the :global and :language levels.
    - Consider :local level, which registers the km in the current document.
  - Unregister keymaps at the same levels.
  - Mapper that can be attached to UI controls.

## UI
  - Initial creation.
  - Event Handling plumbing using keymaps.
    - Alternatives:
    1. Create a custom event handler that can be hooked to any UI control.
      - Abstract the event handling => more work.
      - The events have to be mapped anyway.
    2. Use the native event handling model (swing/gtk+) and adapt to it.
      - Would mean a high coupling with the underlying UI implementation.
      - More work in the long run?
