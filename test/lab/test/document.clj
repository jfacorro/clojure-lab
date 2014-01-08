(ns lab.test.document
  (:refer-clojure :exclude [name replace])
  (:use clojure.test
        [lab.test :onle [->test ->is]]
        lab.model.document)
  (:require [clojure.java.io :as io]
            [lab.model.history :as h]
            [lab.core.lang :as lang]))
;---------------------------
(def file-content "Temp file, should be deleted.")
(def tmp-file "./tmp")
(def default-lang lang/plain-text)
;---------------------------
(defn temp-document-config
  [f]
  (try
    (binding [*untitled-count* (atom 0)]
      (spit tmp-file file-content) ; create temp file
      (f))
      (finally
        (when (-> tmp-file io/file .exists)
          (io/delete-file tmp-file)))))
;---------------------------
(use-fixtures :each temp-document-config)
;---------------------------
(deftest document-creation
  (is (= "Untitled 1" (name (document default-lang))))
  (is (= "" (text (document default-lang)))))
;---------------------------
(deftest document-manipulation
  (let [end      "Oh yes, it will!"
        middle   "Do you think so?"
        len      (count file-content)]
    (->test
        ; Check new document properties
        (document default-lang)
        (->is = false modified?)
        (->is = "" text)
        (->is = 0 length)
        (->is = 1 line-count)
        (->is = "Untitled 1" name)

        ; Bind the document to a file
        (bind tmp-file)
        (->is = false modified?)
        (->is = file-content text)
        (->is = "tmp" name)

        ; Append text to the document
        (append end)
        (->is = true modified?)
        (->is = (str file-content end) text)

        ; Insert text in the middle
        (insert len middle)
        (->is = (str file-content middle end) text)

        ; Delete text from the middle
        (delete len (+ len (count middle)))
        (->is = (str file-content end) text)
        
        ; Save file, check file-content and modified
        (->is not= (slurp tmp-file) text)
        (save)
        (->is = (slurp tmp-file) text)
        (->is = false modified?)

        ; Check line count
        (append "\n")
        (->is = 2 line-count)
        (append "\n")
        (->is = 3 line-count))))
;---------------------------
(deftest bind-non-existing-file
  (is (= "bla" (-> (document default-lang) (bind "./bla") name))))
;---------------------------
(deftest search-and-replace
  (let [doc (append (document default-lang) "abc\nabc\nd")]
    ; Search
    (->test
      doc
      (search "b")
      (->is = [[1 2] [5 6]]))
    ; Replace
    (->test
      doc
      (replace "b" "1")
      (->is = "a1c\na1c\nd" text)
      (replace "1" "bla")
      (->is = "ablac\nablac\nd" text)
      (replace "blac" "bc")
      (->is = "abc\nabc\nd" text))))
;---------------------------
(deftest undo-redo
  (->test (document default-lang)
    (append "abc\nabc\nd")

    ; undo/redo replace
    (replace "b" "1")
    (replace "1" "bla")
    (h/undo)
    (->is = "a1c\na1c\nd" text)
    (h/undo)
    (->is = "abc\nabc\nd" text)
    (h/redo)
    (->is = "a1c\na1c\nd" text)
    (h/redo)
    (->is = "ablac\nablac\nd" text)

    ; undo/redo delete
    (delete 0 4)
    (->is = "c\nablac\nd" text)
    (h/undo)
    (->is = "ablac\nablac\nd" text)
    (h/redo)
    (->is = "c\nablac\nd" text)

    ; undo/redo insert
    (insert 1 "ba")
    (->is = "cba\nablac\nd" text)
    (h/undo)
    (->is = "c\nablac\nd" text)
    (h/redo)
    (->is = "cba\nablac\nd" text)))
;---------------------------
