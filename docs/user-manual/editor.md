The **Editor** is the control you use to modify the content of your files. In general, every tab that's located in the central section of the application is most likely an **Editor**.

By default every editor shows the number of each line in your file, you can hide these by pressing <kbd> Ctrl </kbd> + <kbd> L </kbd>.

<img src="file:///home/jfacorro/dev/clojure-lab/docs/screenshots/editor-no-line-numbers.png" /> 

And show them again using the same key combination.

<img src="file:///home/jfacorro/dev/clojure-lab/docs/screenshots/editor-line-numbers.png" />

## Undoing and Redoing Changes

It is possible for you to **undo** or **redo** an unlimited number of operations on a file. These two commands can be applied through the **Edit > Undo** and **Edit > Redo** menu items or through their respective shortcuts, <kbd> Ctrl </kbd> + <kbd> Z </kbd> and <kbd> Ctrl </kbd> + <kbd> Y </kbd>.

## Language Support

Every file has a language associated to it, the default language for a new file is the plain text language. There are currently only two other languages available which are [Clojure](Clojure) and [Markdown](Markdown). Each language specifies details such as how to format the syntax highlighting, availabe operations on their structures and such things. For example **Markdown** provides shortcuts to apply emphasis to a piece of text or to apply code format to it.

Languages are currently assigned solely based on the document's path extension, `.md` for **Markdown** and `.clj` or `.cljs` for **Clojure**.

## Contextual Help

While editing your file you can get a quick reminder of all available commands by pressing <kbd> F1 </kbd>. Listed commands will also include the ones loaded based on the file's associated language.