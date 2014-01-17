# Clojure Lab

<img src="https://raw.github.com/jfacorro/clojure-lab/master/resources/logo.png" width="200" style="float:right" />

**Clojure Lab** is a project that aims to build a development environment for Clojure in Clojure, its main goals are *usability* and *extensibility*. This project is currently under development, so most of the assertions that follow are closer to wishful thinking than reality... for now :).

### Usability

- When new to the environment you should feel comfortable while discovering and interacting with the features available.
- There should be no road blocks and work should flow naturally once you are familiarized with the way things work.
- Enable the best possible use of the keyboard and mouse combination, both are useful tools for interaction and they each should have their place while working in the environment.

### Extensibility

- Provide a simple API to create your own tools and controls that can be used in the environment, for example in case there's a tool that you are missing and you feel like scratching your own itch.
- New types of projects and documents can be created with plugins.
- Plugins will have access to every core library, so that existing functionality can be enhanced and built upon.

## Rationale

When I started using Clojure I bumped into the situation where I didn't quite know what to use for Clojure development. [Emacs][6] seemed too big a challenge to start playing with a new language, so while reading the [Joy of Clojure][7] and going through its examples, [Clooj][4] was a pretty good tool to explore the basics. Although Clooj has its shortcomings it provides a friendly, simple and familiar interface.

After some time of Clojure development three things happened that brought **Clojure Lab** into being:

- Having to present a final project for me to graduate from school.
- Reading [Chas Emerick][2]'s post [The Ideal Clojure Development Environment][1].
- Thinking that I didn't have an accessible choice for a Clojure development environment that would scale for bigger projects.

Even when I discovered [Counterclockwise][3] and [Light Table][5] was annouced , I still decided to try and build an IDE that could cover as many of the features mentioned in [The Ideal Clojure Development Environment][1] as possible. The final goal is to have a powerful extensible tool, yet simple and accessible, that provides a reactive user interface with discoverable features (a lot easier said than done, right?).

## Running the Application

There are currently two runnable application: **proto**(type) and **lab**.
The entry points for each of these are in the namespaces `proto.main` and `lab.main` respectively.

    $ lein repl
    user> (use 'proto.main)
    user> (-main)

    $ lein repl
    user> (use 'lab.main)
    user> (-main nil)

## Features

Most of the features in the following list were extracted from the post [The Ideal Clojure Development Environment][1]:

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
    - Being able to `print` non-textual data, like images and such.
- Static analysis
    - Symbol navigation (i.e. "Go to declaration").
    - Find usages.

# License

Copyright © 2013 Juan Facorro

Distributed under the Eclipse Public License, the same as Clojure.

  [1]: http://cemerick.com/ideal-clojure-development-environment/
  [2]: http://cemerick.com/
  [3]: https://code.google.com/p/counterclockwise/
  [4]: https://github.com/arthuredelstein/clooj
  [5]: http://www.lighttable.com/
  [6]: http://www.gnu.org/software/emacs/
  [7]: http://joyofclojure.com/
