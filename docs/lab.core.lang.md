# Languages

Languages define the basic functionality associated with a document. The language that corresponds to a document is determined based on the `:rank` function the language defines. This function returns a number between 0 and 1 that determines the likelihood that the document content's is in that language. 

All available languages are taken into consideration and the one that provides the highest likelihood is associated to the document. If there are two or more different languages that provide the same likelihood it is undetermined which one will be chosen. The most basic implementation for a `rank` function is included in `lab.core.lang` which consists of checking the file extension, returning `1` for a match and `0` otherwise.

A default language is defined in the application's configuration map under the key `:default-lang`. The value that should be set under this key is the corresponding language `:id` field. If this is not customized in the configuration, the default language is `:plain-text`.

## Parse tree

All documents have a language associated and every language can specify it own grammar using the syntax defined in the parsley library. Therefore it is possible to get the parse tree for those document where their languages have a well defined grammar. This parse tree is generated incrementally, again thanks to parsley.

This parse tree can be used to do static analysis, syntax highlighting or autcompletion on the document's code (all of which are being currently done). A related gotcha is that there's a good chance that large documents take up too much space in memory, due to the tree and document representation used by the library.

The parse tree for a document can be obtained with the `lab.core.lang/parse-tree` function.

## Language definition

A language in its most basic form consists of the following fields:

    - `:id`: a unique identifier.
    - `:name`: descriptive name.
    - `:options`: parsley parser options.
    - `:grammar`: parsley grammar specification.
    - `:rank`: function that receives a file path and returns a likelihood between 0 and 1.
    - `:styles`: a map containing font styles as values and the names of terminals and non-terminals of the grammar as keys.