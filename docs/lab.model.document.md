# Documents

The application keeps a set of all the opened documents and the current document being edited. The most defining attribute of a document is its language since it determines the way it will be parsed and some the functionality available by default when working on it. The language assigned to a document is defined based on the `:rank` function defined for each language (for more information see [languages][1]).

An incremental buffer (taken from [parsley][1]) is associated to each document at the time of its creation. The buffer is the text representation of the document's content and is incrementally parsed according to the language defined for the document, so you can always get an up to date parse tree for any given document.

## History

Every document keeps a history of the operations that are applied to it, which makes it trivial to implement the redo and undo functionalities. Each item in the history is composed of entities that implement the `lab.model.history.Undoable` protocol. When operations are not meant to be added to the history (e.g. executing operations that redo or undo others) the `with-no-history` macro is used.

Multiple operations can be bundled up as a single one using the `bundle-operations` macro, this allows to encapsule related modifications to the document in a single operation, which comes in handy when implementing some operations (e.g paredit text editing commands).

  [1]: ./lab.core.lang.md
  [2]: https://github.com/cgrand/parsley/
