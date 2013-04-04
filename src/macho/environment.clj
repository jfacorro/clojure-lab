(ns macho.environment)

(def documents
  "Set of current working documents."
  (atom (sorted-set)))

(def current-document
  "Current active document."
  (atom nil))