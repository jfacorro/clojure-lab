# User Manual

Once the application start you see the following screen:

![][initial-screen]

A very simple and clean start up screen with a **File Explorer** control on the left and a main menu on the top.

## Getting Help

At any moment, you can get a list of the commands available by pressing <kbd>F1</kbd>. This will show you a list of commands, their descriptions and shortcuts that you press to use them.

![][help]

## File Explorer

This control allows you to browse and open any number of directories, which will be added as items and will let you access in a quick and practical way, the files you will be working with. To add a directory simply press <kbd>Ctrl</kbd>+<kbd>D</kbd> or the button **Add dir** on the top of the control. 

![][file-explorer]

To remove a directory you no longer want to work with select it and then press <kbd>Del</kbd>, this will only remove it from the control, not your file system. The dialog is closed by pressing <kbd>Esc</kbd>.

![][file-explorer-remove]

Appart from being able to open a file through a traditional file dialog, the **File Explorer** provides a fast file access command in which you can type in any part of the name of the file you want to open and a list of possible matches will be offered. The search is done in all chlidren of the added directories, if none is added then it will be done on the directory and all subdirectories from which the application was started. This fast file search can be opened through the shortcut <kbd>Ctrl</kbd>+<kbd>Alt</kbd>+<kbd>O</kbd>.

![][file-explorer-search-open]

## Editing Documents

Every document has a language associated to it, the default language for a new document is the `:plain-text` language. There are currently only two other languages available which are `:clojure` and `:markdown`. Each language determines the syntax highlighting and other commands included in itself. For example `:markdown` provides shortcuts to apply emphasis or mark text as code.

Languages are currently assigned solely based on the document's path extension, `.md` for **Markdown** and `.clj` or `.cljs` for **Clojure**. The following image shows the looks of a Markdown and a Clojure file being edited.

![][editing-markdown =200x]

![][editing-clojure =200x]

It's worth repeating that while editing your documents you can get a quick reminder of all available commands by pressing <kbd>F1</kbd>.

  [initial-screen]: https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/initial-screen.png
  [help]: https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/help.png
  [file-explorer]: https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/file-explorer.png
  [file-explorer-remove]: https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/file-explorer-remove.png
  [file-explorer-search-open]: https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/file-explorer-search-open.png
