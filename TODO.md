- UI:
  - Abstraction for events
    - Define available events for each component (alla seesaw)
  - Generate hierarchy (ad-hoc?) for the different types of components so that common attributes share the same logic (and implementation code).
  - Key Bindings
    - Remove all defaults and replace them all?
    - Override/replace the ones that are not good enough (e.g. CTRL+TAB for tabbed pane)?

- Model:
  - Complete Document
    - Keep track of changes (implement history or some scheme where changes are registered and kept)
      - Undo/Redo
      - Allow incremental view updates.
  - Implement Project
  - Implement Environment

- App (Control):
  - Define the way plugins/add-ons are loaded.
    - Additionally the way key bindings are defined.
      - Are they always defined globally?
      - How is a plugin's functionality encapsulated? Is it bound to a specific type of file/function/code?
    
