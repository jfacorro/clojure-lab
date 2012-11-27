(ns macho.ui.swing.undo
  (:import [javax.swing.undo UndoManager]
           [javax.swing.event UndoableEditListener]))
;--------------------------------------
(defn on-undoable [doc undo-mgr]
  (let [len  (atom 0)
        mgr  (proxy [UndoManager] []
               (undoableEditHappened [e]
                 (let [edit   (.getEdit e)
                       doclen (.getLength doc)
                       diff   (- @len doclen)]
                   ;(println @len diff (Math/abs diff) (.getType edit) (.getPresentationName edit))
                   (reset! len doclen)
                   (when (pos? (Math/abs diff))
                     (.addEdit undo-mgr edit)))))]
    (.addUndoableEditListener doc mgr)))