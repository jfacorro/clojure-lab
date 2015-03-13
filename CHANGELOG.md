# Change Log

## [Unreleased](https://github.com/jfacorro/clojure-lab/tree/HEAD)

[Full Changelog](https://github.com/jfacorro/clojure-lab/compare/v0.1.0-beta...HEAD)

**Fixed bugs:**

- Update User Manual images urls and complete REPL documentation [\#65](https://github.com/jfacorro/clojure-lab/issues/65)

**Closed issues:**

- Markdown: add shortcuts for links and keystrokes [\#62](https://github.com/jfacorro/clojure-lab/issues/62)

- User Manual [\#61](https://github.com/jfacorro/clojure-lab/issues/61)

**Merged pull requests:**

- \[Closes \#65\] Change URLs [\#66](https://github.com/jfacorro/clojure-lab/pull/66) ([jfacorro](https://github.com/jfacorro))

- \[Closes \#61\] User Manual. [\#64](https://github.com/jfacorro/clojure-lab/pull/64) ([jfacorro](https://github.com/jfacorro))

- \[Closes \#62\] Added shortcuts. Fixed deps issue. [\#63](https://github.com/jfacorro/clojure-lab/pull/63) ([jfacorro](https://github.com/jfacorro))

## [v0.1.0-beta](https://github.com/jfacorro/clojure-lab/tree/v0.1.0-beta) (2014-06-27)

[Full Changelog](https://github.com/jfacorro/clojure-lab/compare/v0.1.0-alpha...v0.1.0-beta)

**Implemented enhancements:**

- Create a short manual/guide [\#51](https://github.com/jfacorro/clojure-lab/issues/51)

- After saving a doc, if the language changes update the view [\#47](https://github.com/jfacorro/clojure-lab/issues/47)

- Show "Find in Files" results tree expanded by default [\#44](https://github.com/jfacorro/clojure-lab/issues/44)

- Improve information displayed by contextual help [\#43](https://github.com/jfacorro/clojure-lab/issues/43)

- Inline docstrings [\#42](https://github.com/jfacorro/clojure-lab/issues/42)

- Implement a way to specify the order of the menu items when defining a :global keymap [\#39](https://github.com/jfacorro/clojure-lab/issues/39)

- Remove confirmation before exiting... [\#37](https://github.com/jfacorro/clojure-lab/issues/37)

- Modify the key used in the :lang-plugins configuration map. [\#27](https://github.com/jfacorro/clojure-lab/issues/27)

- Some plugins should be activated explicitly... [\#23](https://github.com/jfacorro/clojure-lab/issues/23)

**Fixed bugs:**

- Markdown preview plugin is not being loaded [\#48](https://github.com/jfacorro/clojure-lab/issues/48)

- ui: there is a white line in the top of all :tabs components [\#46](https://github.com/jfacorro/clojure-lab/issues/46)

- CompilerException on Ubuntu 14.04 [\#45](https://github.com/jfacorro/clojure-lab/issues/45)

- UI - Make local attr override any attr set through a stylesheet [\#41](https://github.com/jfacorro/clojure-lab/issues/41)

- lab.core.keymap/commands should return only top-most bindings [\#38](https://github.com/jfacorro/clojure-lab/issues/38)

- Find & Replace: searching for the empty string causes StackOverflow. [\#33](https://github.com/jfacorro/clojure-lab/issues/33)

- bundle-operations macro fails with "Too many arguments to if" [\#32](https://github.com/jfacorro/clojure-lab/issues/32)

- Search & Open File doesn't filter out directories [\#30](https://github.com/jfacorro/clojure-lab/issues/30)

- paredit: closing delimiter not inserted [\#29](https://github.com/jfacorro/clojure-lab/issues/29)

- nREPL plugin: lein command not being found in linux [\#28](https://github.com/jfacorro/clojure-lab/issues/28)

- Global plugin commands are not being loaded as menu items [\#26](https://github.com/jfacorro/clojure-lab/issues/26)

- After opening search dialog, close button behavior from tabs changes [\#25](https://github.com/jfacorro/clojure-lab/issues/25)

- Some plugins should be activated explicitly... [\#23](https://github.com/jfacorro/clojure-lab/issues/23)

**Closed issues:**

- paredit: unbalanced delete with strings [\#31](https://github.com/jfacorro/clojure-lab/issues/31)

- dependencies badge [\#11](https://github.com/jfacorro/clojure-lab/issues/11)

## [v0.1.0-alpha](https://github.com/jfacorro/clojure-lab/tree/v0.1.0-alpha) (2014-03-07)

[Full Changelog](https://github.com/jfacorro/clojure-lab/compare/prototype...v0.1.0-alpha)

**Implemented enhancements:**

- Improve performance of rainbow delimiters plugin [\#24](https://github.com/jfacorro/clojure-lab/issues/24)

- Implement lazy loading for the file explorer [\#14](https://github.com/jfacorro/clojure-lab/issues/14)

- Implement stylesheets [\#7](https://github.com/jfacorro/clojure-lab/issues/7)

- lab.ui.select - Implement a way to get ALL components matching a selector [\#5](https://github.com/jfacorro/clojure-lab/issues/5)

- Close button from doc tab shows image with border from button \(Ubuntu\) [\#3](https://github.com/jfacorro/clojure-lab/issues/3)

**Fixed bugs:**

- Cancelling the save dialog closes a modified not saved document [\#22](https://github.com/jfacorro/clojure-lab/issues/22)

- paredit: open-delimiter in comment inserts a closing delimiter... [\#21](https://github.com/jfacorro/clojure-lab/issues/21)

- paredit: wrap-around generates NPE when caret position is at the end of the text [\#20](https://github.com/jfacorro/clojure-lab/issues/20)

- Application freezes when on Clojure document and entering \#" [\#19](https://github.com/jfacorro/clojure-lab/issues/19)

- On click of tree items throws NPE when none is selected [\#18](https://github.com/jfacorro/clojure-lab/issues/18)

- NullReferenceException when using the menu's Save command... [\#17](https://github.com/jfacorro/clojure-lab/issues/17)

- NullReferenceException when cancelling file explorer dialog [\#16](https://github.com/jfacorro/clojure-lab/issues/16)

- Opening a file from the file explorer doesn't work... [\#15](https://github.com/jfacorro/clojure-lab/issues/15)

- dbl-click on the file explorer with no selection results in NullPointerException [\#13](https://github.com/jfacorro/clojure-lab/issues/13)

- Error when running the Gtk test \(lab.ui.gtk.test\) [\#10](https://github.com/jfacorro/clojure-lab/issues/10)

- Menus are not being added to the main frame. [\#9](https://github.com/jfacorro/clojure-lab/issues/9)

- Stylesheets don't apply to all matching component [\#8](https://github.com/jfacorro/clojure-lab/issues/8)

- Hiccup format does not work when not specifying attribute map [\#2](https://github.com/jfacorro/clojure-lab/issues/2)

**Closed issues:**

- Highlighting doesn't seems to work in windows for the prototype [\#12](https://github.com/jfacorro/clojure-lab/issues/12)

- lab.ui.select - Path generation from node not working  [\#6](https://github.com/jfacorro/clojure-lab/issues/6)

- After adding a tab container in the left section documents open in a tab in this control   [\#4](https://github.com/jfacorro/clojure-lab/issues/4)

- :tab component doesn't work when no component is defined for the :-header attribute [\#1](https://github.com/jfacorro/clojure-lab/issues/1)

## [prototype](https://github.com/jfacorro/clojure-lab/tree/prototype) (2013-04-16)



\* *This Change Log was automatically generated by [github_changelog_generator](https://github.com/skywinder/Github-Changelog-Generator)*