## TODO

  - UI
    - Key Bindings
      - Remove all defaults and replace them all?
      - Override/replace the ones that are not good enough (e.g. CTRL+TAB for tabbed pane)?
  
    - Code Editor (or use RSyntaxTextArea and adapt it):
      - Indent code.
      - Comment / Uncomment lines.
      - Find/replace (in an open file, in all files from the file explorer, in all file of a selected folder).
      - Listen for changes in opened files.

    - File Explorer
      - Listen for changes in current dir structure.

    - Rainbow parens
    - Paredit

    - Menu
      - Define a way to specify the order of the menu items.
    
  - App (Control)
    - Establish the way key bindings are defined.
      - Are they always defined globally?
        - Example from emacs -> set-global-key modifies the global-map (which contains all global key bindings).
      - How is a plugin's functionality encapsulated? Is it bound to a specific type of file/function/code?
        - Link plugin with text-editor based on arbitrary definitions.
    - Link Document to ui/text-editor.
      1. Detect updates from Document and impact in editor. \__ Which one? Based on the test done in the prototype, (1) is the
      2. Detect updates form editor and impact in Document. /              one that works best, in (2) model and view can get out of sync.
      - (a) is the way to go:
        - Default keystrokes handlers from editor should be overriden/hijacked/short-circuited.

## DONE


  - UI
    - Abstraction for events
      - Define available events for each component (alla seesaw)
    - Generate hierarchy (ad-hoc?) for the different types of components so that common attributes share the same logic (and implementation code).
    - Gracefuly resolve the properties that don't actually exist in the **implementation** but that we want to have in the **abstraction**.
      - This would allow better component composition (i.e. header and title in the Tab class).
    - Implement Enlive selectors.
    - Modify the global atom created for the ui.
    - Create with-id macro or something similar in order to be able to avoid declaring explicit ids if it's not necessary.
    - The abstraction value for the implementations should be updated with each modification to the abstraction.

    - Code Editor (or use RSyntaxTextArea and adapt it):
      - Syntax high-lighting.
        - Incremental.
        - Strings.
        - Comments.
      - Line numbers (show/hide).
      - Balance delimiters: ( \[ {
      - Mark corresponding delimiter.

    - File Explorer
      - Load directories lazily

    - Unify protocol Text (there's one in buffer and another in UI).
      - So that text operations are defined in a single protocol.

    - Parsley
      - Modify parsley in order to be able to find node index but keeping log(n).
      - Modify parsley/or the document in order to get in O(1) the text from the incremental buffer.
        - Didn't modify parsley but improved the way in which the tokens are searched incrementally.

  - Model
    - Complete Document
      - Keep track of changes (implement history or some scheme where changes are registered and kept in a clojure ordered data
 structure)
        - Allow incremental view updates.
        - Undo/Redo

  - App
    - Define the way plugins/add-ons are loaded.