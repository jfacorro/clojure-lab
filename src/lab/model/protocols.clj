(ns lab.model.protocols)

(defprotocol Undoable
  (undo [this])
  (redo [this]))

(defprotocol File
  (open [this path])
  (save [this] [this path])
  (close[this]))

(defrecord Document)

(defprotocol Project
  (add-file [this doc])
  (delete-file [this doc])
  (exclude-file [this doc]))
  
(defprotocol Workspace
  (add-project [this doc])
  (remove-project [this doc]))

(defprotocol Searchable
  (search [this pattern])
  (replace [this pattern replacement]))


  

