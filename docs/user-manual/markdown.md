Nowadays there's a widespread usage of the **Markdown** language when writing all kinds of documents. For example, StackOverflow and GitHub use it as a default in all their text based content.

Support for **Markdown** in **Clojure Lab** includes:

 - Syntax Highlighting.
 - Shortcuts for formatting text (emphasis, links and code).
 - Code Outline.
 - Preview of the file in HTML form.

## Syntax Highlighting

This feature is always useful for identifying the different elements in the file visually. It is implemented as part of the **Markdown** language so you don't need to do anything in order to enable it.

<img src="file:///home/jfacorro/dev/clojure-lab/docs/screenshots/markdown-syntax-hl.png" />

## Shortcuts

Available shortcuts let you add different types of emphasis and format a piece of text as code:

 - <kbd> Ctrl </kbd> + <kbd> B </kbd>: apply strong format to selection.
 - <kbd> Ctrl </kbd> + <kbd> I </kbd>: apply emphasis format to selection.
 - <kbd> Ctrl </kbd> + <kbd> K </kbd>: format selection as code.
 - <kbd> Alt </kbd> + <kbd> K </kbd>: format selection as a keyboard stroke.
 - <kbd> Alt </kbd> + <kbd> L </kbd>: format selection as a link.

When there is no text selected each shortcut simply inserts the delimiters for each format, levaing the cursor in the middle so you can just type in the content.

## Code Outline

When the **Code Outline** control is activated on a **Markdown** file, the elements displayed as the items in the list are all the titles present. The following is an example of the interaction between a file and the **Code Outline** control.

<img src="file:///home/jfacorro/dev/clojure-lab/docs/screenshots/markdown-code-outline.png" />

As mentioned in the [Code Outline](Code-Outline) page, the list is always synchronized with the contents of the file.

## HTML Preview

It is possible for you to get an HTML preview of your **Markdown** files by pressing the keys <kbd> Ctrl </kbd> + <kbd> P </kbd> while editing the file. This will open a tab in the right section of the application which will show you the preview. 

<img src="file:///home/jfacorro/dev/clojure-lab/docs/screenshots/markdown-preview.png" />

Just as the **Code Outline** is always up to date with the latest changes of the file, the preview is synchronized with its contents as well.
