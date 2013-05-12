<div align="center"><img src="https://raw.github.com/jfacorro/clojure-lab/master/resources/logo.png" width="200" /></div>

**Clojure Lab** is a project that aims to build an IDE for Clojure in Clojure. Its main goals are **usability** and **extensibility**.

This project is currently under development, most of the assertions that follow are closer wishful thinking than reality :).

##Core

###Model

The core of the application handles the loading, representation and manipulation of Documents, Projects and the Environment.

###UI

The core also abstracts all UI components in order to be able to handle specific UI library implementations the same way.

###Control

Basic intialization and loading tasks are done in the control module. Environment initialization, configuration and extensions/plugins loading are some of them.

##Features

All features should be implemented as extensions, but the ones that are closer to the core can be :aot compiled.

Most of the features in the following list were extracted from the post [The Ideal Clojure Development Environment][1] by [Chas Emerick][2]:

###Code Editing
- Syntax highlighting
- Brace, bracket, paren matching.
- Paredit equivalency.
- Easily-togglable rainbow parens.
- S-expression navigation.
- in-place popup macroexpansion
- Auto completion.

###Project Organization
- File management.
- Code compilation.
- Dependencies resolution (maven & leiningen support).
- Initial configuration
- Static analysis
    - Current file code outline.
    - Static namespace browser.

###REPL
- Multiple REPLs support (each running on a separate process).
- Execution history:
    - Search.
    - Execute again.
- Runtime namespace browser.
- Full editor capability in the REPL.
- Automatic generation and configuration of the classpath for local REPLs.

###Nice-to-have items
- Full Java support Integrated (debugging, code completion, profiling, etc.).
- Code generation (deftype/defrecord/extend-type/gen-class/proxy).
- REPL
    - Configurable pretty-printing of output.
    - Being able to “print” non-textual data, like images and such.
- Static analysis
    - Symbol navigation (i.e. "Go to declaration").
    - Find usages.

License

Copyright © 2013 Juan Facorro

Distributed under the Eclipse Public License, the same as Clojure.

  [1]: http://cemerick.com/ideal-clojure-development-environment/
  [2]: http://cemerick.com/
