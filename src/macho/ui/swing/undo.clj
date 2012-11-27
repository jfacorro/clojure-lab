(ns macho.ui.swing.undo
  (:import [javax.swing.event UndoableEditListener]))
;--------------------------------------
(defn on-undoable [doc undo-mgr]
  (.addUndoableEditListener doc undo-mgr))