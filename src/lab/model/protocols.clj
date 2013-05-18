(ns lab.model.protocols)

(defprotocol File
  (open [this path])
  (save [this] [this path])
  (close[this]))

(defprotocol Project
  (add-file [this doc])
  (delete-file [this doc])
  (exclude-file [this doc]))
  
(defprotocol Workspace
  (add-project [this doc])
  (remove-project [this doc]))

