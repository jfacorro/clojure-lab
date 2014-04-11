# Languages

  - Assigns a language to each opened document.
  - A language is assigned based on rules.
    - The most basic one is the extension of the file.
    - Arbitrary rules can be added.
  - There has to be a default language (plain document).
  - Properties
    - :name: descriptive name.
    - description: 
    - options: Parsley parser options.
    - grammar: special forms, literals (numbers, strings, etc.).
    - rank: Predicate function that receives a doc and returns true if the language applies to the doc.
  - Additional functionality can be implemented and associated to the language through plugins.