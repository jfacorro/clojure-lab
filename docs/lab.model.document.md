# Documents

The application keeps a set of all the opened documents and the current document being edited. The most defining attribute of a document is its language since it determines the way it will be parsed and the functionality available when modifying it. The language assigned to a document is defined based on the `:rank` function defined for each language (for more information see [languages][1]).

An incremental buffer (taken from [parsley][1]) is associated to each document at the time of its creation. The buffer is the text representation of the document's content and is incrementally parsed according to the language defined for the document, so you can always get an up to date parse tree for any given document.

Every document keeps a history of the operations that are applied to it, which makes it trivial to implement the redo and undo functionalities.

  [1]: ./lab.core.lang.md
  [2]: https://github.com/cgrand/parsley/