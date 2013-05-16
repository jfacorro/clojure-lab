<img src="https://raw.github.com/jfacorro/clojure-lab/master/resources/logo.png" width="200" style="float:right" />

# Clojure Lab

**Clojure Lab** is a project that aims to build a development environment for Clojure in Clojure. The main goals in **Clojure Lab** are *usability* and *extensibility*.

### Usability
- When new to the environment you should feel comfortable while discovering and interacting with all the features available.
- There should be no road blocks and work should flow naturally once you are familiarized with the way things work.
- Enable the best possible use of the keyboard and mouse combination, both are useful tools for interaction and they each should have their place while working in the environment.

### Extensibility
- There's a simple API you can use to create your own controls to use in the environment in case there's a tool the you are missing and you feel like scratching your own itch.
- New types of projects and documents can be created with plugins.
- Plugins will have access to every library in the core, to so that existing functionality can be enhanced and built upon.

This project is currently under development so most of these assertions are closer to wishful thinking than reality for now :).

## Features

Features will be implemented as extensions or plugins, except for the ones that are closer to the core which should just be part of it, for performance reasons.

Most of the features in the following list were extracted from the post [The Ideal Clojure Development Environment](1) by [Chas Emerick](2):

### Code Editing

- Syntax highlighting
- Brace, bracket, paren matching.
- Paredit equivalency.
- Easily-togglable rainbow parens.
- S-expression navigation.
- in-place popup macroexpansion
- Auto completion.

### Project Organization

- File management.
- Code compilation.
- Dependencies resolution (maven & leiningen support).
- Initial configuration
- Static analysis
    - Current file code outline.
    - Static namespace browser.

### REPL
- Multiple REPLs support (each running on a separate process).
- Execution history:
    - Search.
    - Execute again.
- Runtime namespace browser.
- Full editor capability in the REPL.
- Automatic generation and configuration of the classpath for local REPLs.

### Nice-to-have
- Full Java support Integrated (debugging, code completion, profiling, etc.).
- Code generation (deftype/defrecord/extend-type/gen-class/proxy).
- REPL
    - Configurable pretty-printing of output.
    - Being able to “print” non-textual data, like images and such.
- Static analysis
    - Symbol navigation (i.e. "Go to declaration").
    - Find usages.

# License

Copyright © 2013 Juan Facorro

Distributed under the Eclipse Public License, the same as Clojure.

  [1]: http://cemerick.com/ideal-clojure-development-environment/
  [2]: http://cemerick.com/
