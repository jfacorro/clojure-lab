#The Core of the App

- Document management:
  - Loading a document.
  - Keeps track of the current document that's being worked on.
  - Saving the changes to a modified document.

- Language management:
  - Assigns a language to each opened document.
  - A language is assigned based on rules.
    - The most basic one is the extension of the file.
    - Arbitrary rules can be added.
  - There has to be a default language.

- Plugin management:
  - Load a plugin.
  - Unload a plugin.
  - Keeps track of what plugins have been loaded.
  - Allow plugins to define their own dependencies (?)

- Keymap management:
  - Register keymaps at the :global and :language levels.
    - Consider :local level, though not sure how that would work.
  - Unregister keymaps at the same levels.
  - Mapper that can be attached to UI controls.

- UI:
  - Initial creation.
  - Event Handling plumbing using keymaps.
    - Alternatives:
    1. Create a custom event handler that can be hooked to any UI control.
      - Abstract event handling => more work.
      - The events hace to be mapped anyway.
    2. Use the native event handling model and adapt to it.
      - Would mean a high coupling with the underlying UI implementation.
      - More work in the long run maybe.
