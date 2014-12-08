The ability to find some specific text in the contents of your file is always available as well as being able to replace a piece of this content for another.

You can find all related find and replace commands under the **Edit** menu, which are the following.

## Find

This command will show you a dialog when you press <kbd> Ctrl </kbd> + <kbd> F </kbd> or click the menu item **Edit > Find**.

<img src="file:///home/jfacorro/dev/clojure-lab/docs/screenshots/find-dialog.png" />

Enter the the text you want to find in the text field and then click the **Find Next** button or press the <kbd> Enter </kbd>. When a match is found it will be selected and the cursor will move to the beginning of that selection. 

<img src="file:///home/jfacorro/dev/clojure-lab/docs/screenshots/find-dialog-selection.png" />

By pressing the button or the <kbd> Enter </kbd> key again, **Clojure Lab** will search for the next match, if there is none the cursor will stay put.

<img src="file:///home/jfacorro/dev/clojure-lab/docs/screenshots/find-dialog-selection-next.png" />

A closely related command to **Find** is the **Find Next**. Once you close the **Find** dialog you will be able to repeat the search for the last term you entered there by pressing <kbd> F3 </kbd> or clicking the menu item **Edit > Find Next**.

## Replace

Replacing text in a file works pretty much the same as finding it, the only difference is you also specify the text that will replace the original one. Activate this command by pressing <kbd> Ctrl </kbd> + <kbd> H </kbd> or cliking **Edit > Replace**.

<img src="file:///home/jfacorro/dev/clojure-lab/docs/screenshots/replace-dialog.png" />

To perform a replacement follow these steps:

1. Enter the text that will be replaced in the **Find** field.
2. Enter the replacement text in the **Replace** field.
3. Press the **Find Next** button to find a match.
4. If a match is found press the **Replace** button to apply the replacement and search for the next match.

Alternatively you can press the **Replace All** button which will perform a replacement for all matches. This is useful when you are sure that every ocurrence of the text should be replaced.

## Find in Files

Since sometimes you need to find text in multiple files the **Find in Files** command lets you do just that. You will see the following dialog after pressing <kbd> Alt </kbd> + <kbd> F </kbd> or cliking the menu item **Edit > Find in Files**.

<img src="file:///home/jfacorro/dev/clojure-lab/docs/screenshots/find-in-files-dialog.png" />

As in the previous commands, there is a **Find** field where you enter the text you need to find and there's also a **Browse** button in order for you to select a directory. The search will include all files in this directory and will also include all children directories if you choose to do so by activating the **Recursive** checkbox.

<img src="file:///home/jfacorro/dev/clojure-lab/docs/screenshots/find-in-files-dialog-complete.png" />

Once you have added all the necessary information, pressing the **Find All** button will open a tab in the lower section of **Clojure Lab**. In this tab you will find the results of the search in the form of a tree.

<img src="file:///home/jfacorro/dev/clojure-lab/docs/screenshots/find-in-files-result.png" />

Each leaf in the tree is a match of the search, to open the file where the match was found, double-click the item. This will not only open the file (if it was not already open) but it will also move the cursor to the position where the match is located.