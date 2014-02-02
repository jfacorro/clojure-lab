## TODO

### UI

#### Tabs
  - Higlight the current one.
  - Change the default wrapping mode: given the way the current L&F is overriden the arrows to navigate tabs in Scroll mode don't show up.

#### Events
  - Close the channels that listen to components that don't exist anymore.
  - Key Bindings
    - Remove all defaults and replace them all => not practical
    - Override/replace the ones that are not good enough (e.g. CTRL+TAB for tabbed pane)?
  - Implement listen/unlisten functions to add and remove event handlers from a component.
#### Code Editor
  - Move the following code editor features to their own plugin.
This could be done by adding a hook to an editor creation templates function.
    - Syntax highlighting.
    - Delimiter matching.
    - Undo/redo (?)
  
  - Features:
    - Indent code.
    - Find/replace (in an open file, in all files from the file explorer, in all file of a selected folder).
    - Listen for changes in opened files (i.e. "The file has been modified fo you want to reload it?").
    - Go to line.
    - Wrap text.

#### File Explorer
  - Listen for changes in current dir structure.

#### Code Outline:
  - Add ENTER as a trigger for go-to-definition.
  - When going to line, position the line in the top of the scroll.
    - Depends on the implementation of the editor's Go to line.

#### REPL
  - Use nrepl, not a process and its input/output streams.
  - Fix bug: when closing the REPL tab, even if the user enters no or cancel in the prompt, the
  tab closes all the same, since the on-click handler from the template is used as well.
    - Possible solutions:
      1. Implement an event handlers map for each component and a listen! function for the UI.
      2. Remove the init in the templates function, so that it only returns the hiccup style vector AND instead of just proxying the add protocol functions, have the function in lab.ui.core call hiccup->component.  

#### Rainbow parens
  - Implement.

#### Paredit
  - Implement.

#### Menu
  - Define a way to specify the order of the menu items.

### Model
  - Document
    - (?) Add a list of views to the document in order to be able to track them and update them (e.g. in the redo/undo scheme).

### App (Control)
    - Plugin management:
      - Unload a plugin.
      - (?) Allow plugins to define their own dependencies.
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
      - Comment / Uncomment lines.

    - File Explorer
      - Load directories lazily

    - Unify protocol Text (there's one in buffer and another in UI).
      - So that text operations are defined in a single protocol.

    - Parsley
      - Modify parsley in order to be able to find node index but keeping log(n).
      - Modify parsley/or the document in order to get in O(1) the text from the incremental buffer.
        - Didn't modify parsley but improved the way in which the tokens are searched incrementally.

    - REPL
      - Create plugin.

  - Model
    - Complete Document
      - Keep track of changes (implement history or some scheme where changes are registered and kept in a clojure ordered data
 structure)
        - Allow incremental view updates.
        - Undo/Redo

  - App
    - Define the way plugins/add-ons are loaded.
