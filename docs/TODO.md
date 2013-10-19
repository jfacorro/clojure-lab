TODO
====

  UI
  --
  
  - Create with-id macro or something similar in order to be able to avoid declaring explicit ids if it's not necessary.
  - Abstraction for events
    - Define available events for each component (alla seesaw)
  - Key Bindings
    - Remove all defaults and replace them all?
    - Override/replace the ones that are not good enough (e.g. CTRL+TAB for tabbed pane)?
  - The abstraction reference for the implementations should be updated with each modification to the abstraction.
    - ...or have a reference for the abstract (not).
  
  - Code Editor (or use RSyntaxTextArea and adapt it):
    - Indent code.
    - Line numbers (show/hide).
    - Comment / Uncomment lines.
    - Syntax high-lighting.
      - Incremental.
      - Strings.
      - Comments.
      - Balance delimiters: ( [ {
      - Mark corresponding delimiter.
  
  Model
  -----
  
  - Complete Document
    - Keep track of changes:
      - Allow incremental view updates.
  - Implement Project.
  - Implement Environment.
  
  App (Control)
  -------------
  
  - Define the way plugins/add-ons are loaded.
  - Establish the way key bindings are defined.
    - Are they always defined globally?
      - Example from emacs -> set-global-key modifies the global-map (which contains all global key bindings).
    - How is a plugin's functionality encapsulated? Is it bound to a specific type of file/function/code?
      - Link plugin with text-editor based on arbitrary definitions.
  - Link Document to ui/text-editor.
    a. Detect updates from Document and impact in editor. \__ Which one? Based on the test done in the prototype, (a) is the
    b. Detect updates form editor and impact in Document. /              one that works best, in (b) model and view can get out of sync.
    
Done
====
  - Generate hierarchy (ad-hoc?) for the different types of components so that common attributes share the same logic (and implementation code).
  - Gracefuly resolve the properties that don't actually exist in the **implementation** but that we want to have in the **abstraction**.
    - This would allow better component composition (i.e. header and title in the Tab class).
  - Implement Enlive selectors.
  - Modify the global atom created for the ui.

  Model
  -----
  
  - Complete Document
    - Keep track of changes (implement history or some scheme where changes are registered and kept in a clojure ordered data structure)
      - Undo/Redo
