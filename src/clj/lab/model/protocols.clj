(ns lab.model.protocols)

(defprotocol Text
  (insert     [this offset s] "Inserts s in offset.")
  (append     [this s] "Appends s to the current text.")
  (delete     [this start end] "Delete the contents of the buffer from positions start to end.")
  (length     [this] "Returns the length of the buffer.")
  (text       [this] "Returns the contents of the buffer as a string.")
  (substring  [this start end] "Returns the substring from start to end offsets."))

(defprotocol Parsable
  (parse-tree [this] "Returns a parse tree with each node being {:tag :tag-kw :content [node*]}"))
