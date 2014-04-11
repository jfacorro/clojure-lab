# The Core

The application's core deals with [initialization](#init) and primarily with the following entities:

- Itself (the App).
- [Documents](#documents).
- [Languages](#languages).
- [Keymaps](#keymaps).
- [Plugins](#plugins).

This document is just a fairly brief introduction of the main pieces of functionality on which the whole of *Clojure Lab* is built upon. For a more detailed explantation on how each of these works you should check their specific documents.

<a name="init"></a>
## Initialization

> In the beginning there was nothing...

There's really not that much happening from the core's point of view during initialization, only two main thing actually.

The first one is the fetching of any configuration file that may exist in the current directory where the program was executed. This configuration file contains such things as:

  - `:name`: the application's name, which is used as the main window title.
  - Plugins that should be loaded during initialization.
     - `:core-plugins`: these are the one that define core functionality (e.g. creation of core commands and GUI).
     - `:plugins`: languages, file explorer, etc.
     - `:lang-plugins`: defined as a map which determines the plugins that are automatically loaded when a document of the indicated language is opened.
  - `:plugins-dir`: path to location where plugin source files can be found. This can be an absolute path or relative to the one where the application was started.
  - `:current-dir`: current directory.
  - `:default-lang`: default language to be used when no other is resolved when opening a document.

The second thing that happens is the loading of all plugins listed in the configuration's `:core-plugins` and `:plugins` mentioned above. These plugins in turn have their own initialization functions, but the actions taken by them can vary widely. 

In particular, the `lab.core.main` plugin creates a basic GUI structure, which includes the commands necessary to manipulate documents, plus some other operations like moving from tab to tab, toggling fullscreen mode and some other minor features. There are some additional details related to event handling that are defined in this plugin but these will be addressed in the plugin's own documentation.

## App (TODO)

<a name="documents"></a>
## Documents

Like in most IDEs, documents are the center of all activities when working with *Clojure Lab*. Document loading, modification, saving and closing, are all standard operations that are supported and implemented in the core. Keeping track of the documents that are currently opened, the one that's being used at the moment and switching between the opened documents, are the other sort of operations the core is concerned with.

When opening a document, all existing languages are asked for a ranking number using a `rank` function that they should include in their implementation. The criteria currently in use is based solely on the file extension associated with each document.

(For more information see [Documents][2])

<a name="languages"></a>
## Languages

All languages are defined as plugins except for the plain text languages, which is included in the implementation of the core language library. 

Among other things languages define the grammar that the [Parsley][1] library will use to parse the document, a language specific keymap and a ranking function that is used with each document to determine their language.

(For more information see [Languages][3])

<a name="keymaps"></a>
## Keymaps

These are just mappings between a combination of keystrokes and a command. 

The app has an associated keymap were all global commands are defined. Since these commands are global they are added as menu options in the GUI's menu bar. Plugins can add global commands by defining a `:global` keymap, this allows them to expose a visible point of entry to their functionality.

(For more information see [Keymaps][4])

<a name="plugins"></a>
## Plugins

Almost all features and functionality in **Clojure Lab** are defined in plugins. As mentioned in the [initialization](#init) section, there are a number of plugins that are loaded only once during this process, while others are loaded each time a document is opened according to the configuration defined in the `:lang-plugins` map.

There are two types of plugins available, these are `:global` and `:local`. The former is meant for plugins that define either global functionality (i.e. find & replace, file explorer, messages notifier, etc.) or implement the base mechanisms that are completed whith the usage of behavior defined in other plugins (i.e. code outline).

(For more information see [Plugins][5])

  [1]: https://github.com/cgrand/parsley/
  [2]: ./lab.model.document.md
  [3]: ./lab.core.lang.md
  [4]: ./lab.core.keymap.md
  [5]: ./lab.core.plugin.md
