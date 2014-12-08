**Clojure** is the language for which **Clojure Lab** was originally built for, so much so that the development of the application was done using early versions of itself. 

All files that have the extensions `.clj`, `.cljs` or `.cljx` are considered **Clojure** source code files.

The features associated with these **Clojure** files are the following:

 - Syntax Highlighting 
 - Delimiter Matching
 - Rainbow Delimiters
 - Paredit
 - Autocomplete
 - nREPL
   - Macroexpand
   - Docstring Information

## Syntax Highlighting 

**Clojure**'s syntax higlighting has full support for all of the language elements. Behind the scenes each language currently uses the [parsley](https://github.com/cgrand/parsley) library to figure out how to process the source code in each file. Since **parsley** was built with **Clojure** in mind it fits perfectly for using it with this language.

<img src="https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/clojure-syntax-hl.png" />

## Delimiter Matching

The **Clojure** language does a heavy use of delimiters and sometimes it is pretty hard to identify the pair that matches a given delimiter. This is why it is handy to highlight both delimiters whenever the cursor is positioned right next to one of them.

In the following screenshots you can see how both brackets are highlighted either when the cursor is before or after the closing bracket.

<img src="https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/clojure-delim-match-after.png" />

<img src="https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/clojure-delim-match-before.png" />

This feature is enabled by default for all **Clojure** files.

## Rainbow Delimiters

Rainbow delimiters are a quick and visual way of identifying the nesting level associated with each pair of delimiters. Additionally this also helps figuring out where the code goes wrong when there is some unbalanced delimiter lying around.

Colors are assigned from a fixed set according to the nesting level, since the set has a limited amount of colors, once a deep enough level is reached, the assigned colors cycle form the beginning of the set. For example:

<img src="https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/clojure-rainbow-cycle.png" />

This feature is enabled by default for all **Clojure** files although you can disable it by pressing <kbd> Ctrl </kbd> + <kbd> P </kbd> from inside the editor.

## Paredit

This feature comes from the the world of Emacs. **Paredit** is composed out of a set of commands that make editing Lisp lists a lot easier, therefore the name ( **par**enthesis **edit**or ). 

Although there a too many paredit commands to list here, these are the groups in which they are categorized:

- Basic Insertion.
- Deleting & Killing.
- Movement & Navigation.
- Depth-Changing Commands.
- Barfage & Slurpage.

The main objective of these commands is to keep delimiters balanced, help the navigation between the different levels and assist in the correct modification of depth and/or size for each list.

Please check this [reference card](http://pub.gajendra.net/src/paredit-refcard.pdf) for a more detailed explanation of each command. Also remember you can check what are the available commands by pressing <kbd> F1 </kbd> from the editor.

## Autocomplete

The auto-completion pop-up window is available by pressing the <kbd> Ctrl </kbd> + <kbd> Space </kbd> key combination. You need to position the cursor right after a word in order for the auto-complete to work, otherwise the pop-up won't even be displayed.

<img src="https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/clojure-autcomplete-options.png" />

The information of all available symbols that could match the auto-complete is taken from two sources. 

The first one is the local context from where the auto-complete is requested. In the example above the context would be the `numbers` function which defines both itself and the argument named `n`. Since the cursor is positioned right after the `n` then the possible options for auto-completion are both the function's name and the argument's name. The local context also includes the name of functions declared in the same file.

<img src="https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/clojure-autcomplete-context.png" />

The second source of information is the live running environment provided by the **REPL** which will be explained in the next section.

## REPL

A REPL is in its most basic form a console in which you interact with a running instance of your application. REPL is an acronym for *Read-Eval-Print-Loop* which describes the interaction between you and the console. The use of a REPL is a chractertic feature of hacking in a Lisp dialect and since **Clojure** is one of those, **Clojure Lab** includes a REPL for your programming pleasure.

In order to open a bare REPL that has only the standard **Clojure** libraries avilable you can either press the <kbd> Ctrl </kbd> + <kbd> Alt </kbd> + <kbd> R </kbd> key combination or click on the **Clojure > nREPL > Start and Connect to REPL** menu item. This will open a tab in the lower section of the application which is shown in the following screenshot.

<img src="https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/clojure-nrepl-open.png" />

There are two editor controls in that tab, one above the other. The upper one shows you the information returned from the REPL while in the lower you can type in the code you want to evaluate. After entering the code you can send it to the REPL by pressing the keys <kbd> Ctrl </kbd> + <kbd> Enter </kbd>. The following sequence shows the entering and evaluating of the expression `(println "hello")`.

<img src="https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/clojure-nrepl-code.png" />

<img src="https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/clojure-nrepl-eval.png" />

Both lines signalled by the arrows are the result of evaluating the code sent to the REPL. The first one is the output to **stdout** while the second one is the result of evaluating the expression.

The REPL editor where you enter the code keep a history of all code sent. You can browse through each of the snippets sent from this editor by pressing <kbd> Ctrl </kbd> + <kbd> Up </kbd> to get the previous entry in the history or <kbd> Ctrl </kbd> + <kbd> Down </kbd> for the next one.

Another way of sending code to the REPL is through the editor. Pressing <kbd> Ctrl </kbd> + <kbd> Enter </kbd> with no text selected while in the editor will send the whole content of the current file to the REPL. In the case where you have some text selected then only the selected text will be sent to the REPL.

### Current Namespace

The **Clojure** namespace in which code is evaluated on the REPl is taken from the `ns` declaration of the current file. If there is no `ns` declaration then the current namespace does not change. In order to determine what the current namespace is you can send `*ns*` to the REPL.

### Autocomplete

As mentioned in the **Autocomplete** section the REPL is used as the second source of information when listing the autocompletion symbols. The way this works is by sending a piece of code to the REPL that returns all symbols that are accesible from the current namespace.

These symbols include:

- Imported Java classes.
- Required namespaces.
- Used namespaces.
- Referred symbols.

This allows to include in the autocomplete options a more accurate approach since the information is taken from a live running environment of the application.

### Macroexpand

Having the REPL also enables you to get the macroexpansion of every expression in your code since getting it is as simple as evaluating `macroexpand` with the expression as its arguments.

To get the macroexpansion of an expression just press <kbd> Ctrl </kbd> + <kbd> Alt </kbd> + <kbd> Enter </kbd> and you will get a pop-up window with the expanded code. 

This is what a macroexpansion on the expression `(ns awesome)` looks like:

<img src="https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/clojure-macroexpand.png" />

### Docstring Information

It is also possible to get the docstring information from any Var just as easily by pressing <kbd> Ctrl </kbd> + <kbd> I </kbd>.

Here is the pop-up with the docstring for the `ns` macro:

<img src="https://raw.github.com/jfacorro/clojure-lab/master/docs/screenshots/clojure-docstring.png" />
