Once the application starts you will see a very simple and clean start up screen with a [File Explorer](File-Explorer) control on the left and a main menu on the top.

<img src="https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/initial-screen.png"/>

In the main menu you will find several commands to perform all kinds of actions. The following is a list of the default most important menu items:

- **File**: create, open, save and close files.
- **View**: hide/show controls, modify content layout.
- **Edit**: undo/redo actions, find and replace, etc.

In this **Getting Started** guide we will go through most of the commands under the **File** menu item, remaining commands will be covered in other sections.

## Creating, Editing and Saving a File

In order to create a new file you should either click on the menu item **File > New** or just press the both the <kbd> Ctrl </kbd> + <kbd> N </kbd> key combination.

You will see a new tab in the center section of the application. The title for this document will be **Untitled X**, where **X** is just a number that will increment with each creation of a new document.

<img src="https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/file-new.png"/>

Once you have created the new file you can start editing its contents by just typing into it. You'll notice that once you start modifying the file, a little asterisk will appear to the right of the file's name (in this case **Untitled 2**). This asterisk means there are modifications done to the file that haven't been saved to disk.

<img src="https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/file-edit.png"/>

It is always a good idea to regulary save your changes to disk which you can do either through the **File > Save** menu item or by pressing the keys <kbd> Ctrl </kbd> + <kbd> S </kbd>. If the file is a new one a **Save File Dialog** will appear, which will ask you to select a directory and a name for your file.

<img src="https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/file-save-dialog.png" />

From this point on every time you modify the contents of the file and save these changes, they will be saved to the file you specified in the **Save File Dialog**.

The extension in the name you provide when saving a file tells **Clojure Lab** what type of file it is you are editing. Based on this information **Clojure Lab** can provide other useful functionality while you edit your file. For more information about this and other interesting features, check the manual entry for the [Editor](Editor).

## Opening a File
 
To open en existing file click the menu item **File > Open** this will show an **Open File Dialog** in which you will be able to find the file you want to open. Once you find it, just select it and click the **Open** button.
 
<img src="https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/file-open-dialog.png" />

This will open a tab in the center section of **Clojure Lab** with the contents of the file you selected and showing the filename as the tab's title.

<img src="https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/file-open.png" />

## Closing a File

Once you are done editing a file there are a number of ways of closing it:

- Press the keys <kbd> Ctrl </kbd> + <kbd> W </kbd> to close the current file.
- Click the menu item **File > Close** to close the current file.
- Click on the X button of a specific tab to close the file associated with that tab.

<img src="https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/file-close-tab-button.png" />

If there are changes to the file that haven't been saved **Clojure Lab** will ask you if you would like to save those changes. You can either choose to save them, ignore the changes or cancel the close operation.

<img src="https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/file-close-confirm.png" />

## Working with Multiple Files

It is possible to open any number of files in **Clojure Lab** in order to easily alternate work between them. When more than one file is open, you will see multiple tabs in the center section of the application. The current file on which you are working will be distinguished by a colored line right below the tab.

<img src="https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/file-multiple-tabs.png" />

In this screenshot the current file is the second one, named **Untitled 5**.

## What's Next?

There are many other **Clojure Lab** important features you should explore, the following are just some of them:

- [File Explorer](File-Explorer).
- [Editor](Editor).
- [Code Outline](Code-Outline).
- [Find & Replace](Find-Replace).

For a complete list of features please check the index in the [Home](Home) page.

