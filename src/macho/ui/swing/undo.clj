(ns macho.ui.swing.undo
  (:import [javax.swing.undo UndoManager AbstractUndoableEdit]
           [javax.swing.text DefaultStyledDocument$AttributeUndoableEdit]
           [javax.swing.event UndoableEditListener]))

(defn on-undoable [doc undo-mgr]
  (let [handler (proxy [UndoableEditListener] []
                  (undoableEditHappened [e]
                    (let [edit (.getEdit e)]
                      (when-not (.contains (.getPresentationName edit) "style")
                        (.addEdit undo-mgr edit)))))]
    (.addUndoableEditListener doc handler)))