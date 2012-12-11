(ns macho.ui.swing.undo
  (:import [javax.swing.undo UndoManager]
           [javax.swing.event UndoableEditListener DocumentEvent$EventType]))
;;--------------------------------------
(defn handle-edit
  "Add an undoable handler for the document."
  [undo-mgr e]
  (let [edit   (.getEdit e)
        evtype (.getType edit)]
    (when (not= evtype DocumentEvent$EventType/CHANGE)
      (.addEdit undo-mgr edit))))
;;--------------------------------------
