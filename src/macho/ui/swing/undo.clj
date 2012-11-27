(ns macho.ui.swing.undo
  (:import [javax.swing.undo UndoManager]
           [javax.swing.event UndoableEditListener DocumentEvent$EventType]))
;;--------------------------------------
(defn on-undoable
  "Add an undoable handler for the document."
  [doc undo-mgr]
  (let [mgr  (proxy [UndoManager] []
               (undoableEditHappened [e]
                 (let [edit   (.getEdit e)
                       evtype (.getType edit)]
                   (when (not= evtype DocumentEvent$EventType/CHANGE)
                     (.addEdit undo-mgr edit)))))]
    (.addUndoableEditListener doc mgr)))
;;--------------------------------------
