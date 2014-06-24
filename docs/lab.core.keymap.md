# Keymaps

These entities are used to determine a mapping between a given keystroke and the command that should be executed when it is pressed. They are mainly used in the definition of [plugin][1]s or when assigning a [key handler](#missing-link) to a UI component.

Keystrokes can be expressed as a string with each key that forms a part of it separated by a space (e.g. "ctrl alt del"). A list of all the keys that can be used are in [this][2] document. Additionally characters can be partof a keystroke description, this means that the keystroke `(` is recognized when the combination that would insert a `(` is pressed.

The `shift` is for the moment a special case and can't be used to describe a keystroke. This is due to the fact that a keystroke that results in a character obtained through the use of the shift key (e.g. `shift 9` equals `)` in a given configuration) would map to various expressions (i.e. `shift 9` and `)`) which is ambiguous.

## Plugin Keymaps

When specifying a plugin's keymaps, these can be defined at the `:global`, `:lang`uage and `:local` levels. 

- `:global` keymaps determine the menu items that are shown in the main application's menu, some values included in the keymap's commands determine how these items are added. All of these commands are accesible from within any context.

- `:lang`uage keymaps hold commands that apply to all document of a specific language. They are loaded into the a document's context when it is opened and the language is assigned. 

- `:local` keymaps apply only to a single document.

## Event Keymaps

All keymaps used as event handlers are always considered `:local` and have to be associated to a key event, otherwise the event generated won't contain the information necessary to map to a keystroke.

## Definition

A keymap is defined by providing a name, a type (`:global`, `:lang` or `:local`) and any number of commands that are added with their corresponfing keystrokes.

     (km/keymap "Markdown"
       :local
       {:keystroke "ctrl p" :fn ::show-preview :name "Html Preview"})

Commands are just maps that should contain two mandatory keywords: `:keystroke` and `:fn`. The former should have a keystroke value expressed as a string as explained above. The latter can be either a function, a var or a namespace qualified keyword that maps to an existing var.

Optional keys for commands include the follwing:

- `:name` should be a descriptive name for the commands. In `:global` keymaps this will be used as the text for the menu item. For all keymaps it will be used as the description for the contextual help dialog.
- `:category` is used for `:global` keymaps to determine where the menu item should be added. Menu levels are determined through the use of the character `>` so a command which should inserted under the `File > Projects` should include this string as the value for the `:category` key.

There is a feature which allows to specify the order in which the item in a `:global` keymap should be added, but it's still experimental and not so user friendly for now.

  [1]: ./lab.core.plugin.md
  [2]: ./lab.core.keymap-keys.md