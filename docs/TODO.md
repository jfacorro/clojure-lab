## TODO

### UI

### Ref used in app
  - Change the atom for an agent.
    - This has some deeper implications since all UI actions won't be executed in the UI thread but in the agent's.
    - Unless the ui/action macro is not part of the API but is used in the library internally.

#### Events
  - Create a :keymap attribute for components so that keymaps can be registered to them.

#### Code Editor
  - Move the following code editor features to their own plugin.
    - Undo/redo (?)

  - Features:
    - Indent code.
    - Find/replace (in an open file, in all files from the file explorer, in all files of a selected folder).
    - Listen for changes in opened files (i.e. "The file has been modified fo you want to reload it?").
    - Wrap text.

#### Code Outline:
  - When going to line, position the line in the top (or middle) of the scroll.
    - Depends on the implementation of the editor's Go to line.

#### REPL
  - Use nrepl, not a process and its input/output streams.

#### Paredit
  - Implement
    - Basic Insertion Commands
      - paredit-backslash \
      - paredit-comment-dwim M-;
      - paredit-newline C-j
    - Deleting and killing
      - paredit-forward-delete C-d, delete
      - paredit-backward-delete backsapce
      - paredit-kill C-k
      - paredit-forward-kill-word M-d
    - Miscellaneous Commands
      - paredit-split-sexp M-S
      - paredit-join-sexp M-J

#### Menu
  - Define a way to specify the order of the menu items.
    - Menu item information can be defined as the meta of the command function.

### App (Control)
    - Plugin management:
      - Unload a plugin.
      - (?) Allow plugins to define their own dependencies.

## DONE

### UI
  - Abstraction for events
    - Define available events for each component (alla seesaw)
  - Generate hierarchy (ad-hoc?) for the different types of components so that common attributes share the same logic (and implementation code).
  - Gracefuly resolve the properties that don't actually exist in the **implementation** but that we want to have in the **abstraction**.
    - This would allow better component composition (i.e. header and title in the Tab class).
  - Implement Enlive selectors.
  - Modify the global atom created for the ui.
  - Create with-id macro or something similar in order to be able to avoid declaring explicit ids if it's not necessary.
  - The abstraction value for the implementations should be updated with each modification to the abstraction.

#### Tabs
  - Higlight the current one.
  - Change the default wrapping mode.
    - Not conveninet: given the way the current L&F is overriden the arrows to navigate tabs in Scroll mode don't show up.

#### Events
  - Fixed bug when mapping keys in a JVM with a different language.
  - Key Bindings
    - Remove all defaults and replace them all => not practical
    - Override/replace the ones that are not good enough (e.g. CTRL+TAB for tabbed pane)?
  - Implement listen/ignore functions to add and remove event handlers from a component.
  - Close the channels that listen to components that don't exist anymore.
    - Not necessary since parked go block and their related channels are GC'ed if done right.

#### Code Editor
  - Syntax high-lighting.
    - Incremental.
    - Strings.
    - Comments.
  - Line numbers (show/hide).
  - Balance delimiters: ( \[ {
  - Mark corresponding delimiter.
  - Comment / Uncomment lines.
  - Go to line.
  - Move the following code editor features to their own plugin. This could be done by adding a hook to an editor creation templates function.
    - Syntax highlighting.
    - Delimiter matching.
  - Unify protocol Text (there's one in buffer and another in UI).
    - So that text operations are defined in a single protocol.

#### File Explorer
  - Load directories lazily
  - Listen for changes in current dir structure.

#### Parsley
  - Modify parsley in order to be able to find node index but keeping log(n).
  - Modify parsley/or the document in order to get in O(1) the text from the incremental buffer.
    - Didn't modify parsley but improved the way in which the tokens are searched incrementally.

#### Code Outline:
  - Add ENTER as a trigger for go-to-definition.

#### REPL
  - Create plugin.
  - Fix bug: when closing the REPL tab, even if the user enters no or cancel in the prompt, the
  tab closes all the same, since the on-click handler from the template is used as well.
    - Possible solutions:
      1. Implement an event handlers map for each component and a listen! function for the UI.
      2. Remove the init in the templates function, so that it only returns the hiccup style vector AND instead of just proxying the add protocol functions, have the function in lab.ui.core call hiccup->component.  

#### Rainbow parens
  - Implement.
  - Using #() messes up the depth from then on.
  - Improve performance: maybe avoid using or improve the performance of lang/location.

#### Paredit
  - Implement
    - Basic Insertion Commands
      - paredit-open-round (
      - paredit-open-bracket \[
      - paredit-open-curly {
      - paredit-close-round )
      - paredit-close-bracket ]
      - paredit-close-curly }
      - paredit-close-round-and-newline M-)
      - paredit-meta-doublequote M-"
    - Movement & Navigation
      - paredit-forward C-M-f
      - paredit-backward C-M-b
    - Depth-Changing Commands
      - paredit-wrap-around M-(
      - paredit-splice-sexp M-s
      - paredit-splice-sexp-killing-backward M-<up>, ESC <up>
      - paredit-splice-sexp-killing-forward M-<down>, ESC <down>
      - paredit-raise-sexp M-r
    - Barfage & Slurpage
      - paredit-forward-slurp-sexp C-), C-<right>
      - paredit-forward-barf-sexp C-g, C-<left>
      - paredit-backward-slurp-sexp C-(, C-M-<left>, ESC C-<left>
      - paredit-backward-barf-sexp C-f, C-M-<right>, ESC C-<right>

## Model
  - Complete Document
    - Keep track of changes (implement history or some scheme where changes are registered and kept in a clojure ordered data
 structure)
      - Allow incremental view updates.
      - Undo/Redo

## App
    - Define the way plugins/add-ons are loaded.
    - Link Document to ui/text-editor.
    - Establish the way key bindings are defined.
