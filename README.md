# Clojure Lab

<img src="https://raw.github.com/jfacorro/clojure-lab/master/resources/logo.png" align="right" style="float:right" />

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

### Requirements

In order to fire up Clojure Lab from the REPL, build or run it you need:

- [Leiningen](https://github.com/technomancy/leiningen/) installed and in your `$PATH`.
- [JDK 1.7.0](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html) installed.

### REPL

The application can be run by either using the `lein run` or starting a REPL and then calling the `-main` function:

    $ lein repl
    lab.main> (-main)

### Building

An executable `lab.jar` file can be generated through the use of the `lein build` command. 

### Downloading

Pre-built executable jar files are available in the [releases][9] section of this project.

## User Guide

There's a small user manual/guide that shows how to start using **Clojure Lab** [here][8].

## Features

Most of the features in the following list were extracted from the post [The Ideal Clojure Development Environment][1]:

### Code Editing

- Syntax highlighting. **DONE**
- Brace, bracket, paren matching. **DONE**
- Paredit equivalency. **DONE**
- Easily-togglable rainbow parens. **DONE**
- S-expression navigation.
- In-place popup macroexpansion. **DONE**
- Auto completion. **DONE**

### Project Organization

- File management. **DONE**
- Code compilation.
- Dependencies resolution (maven & leiningen support).
- Initial configuration. **DONE**
- Static analysis:
    - Current file code outline. **DONE**
    - Static namespace browser.

### REPL
- Multiple REPLs support (each running on a separate process). **DONE**
- Execution history:
    - Search.
    - Execute again. **DONE**
- Runtime namespace browser.
- Full editor capability in the REPL.
- Automatic generation and configuration of the classpath for local REPLs. **DONE (Leiningen)**

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

Copyright © 2014 Juan Facorro

Distributed under the Eclipse Public License, the same as Clojure.

  [1]: http://cemerick.com/ideal-clojure-development-environment/
  [2]: http://cemerick.com/
  [3]: https://code.google.com/p/counterclockwise/
  [4]: https://github.com/arthuredelstein/clooj
  [5]: http://www.lighttable.com/
  [6]: http://www.gnu.org/software/emacs/
  [7]: http://joyofclojure.com/
  [8]: https://github.com/jfacorro/clojure-lab/blob/master/docs/manual.md
  [9]: https://github.com/jfacorro/clojure-lab/releases
