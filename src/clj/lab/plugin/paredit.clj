(ns lab.plugin.paredit
  (:require [clojure.zip :as zip]
            [lab.ui.core :as ui]
            [lab.util :refer [timeout-channel find-limits]]
            [lab.model.document :as doc]
            [lab.model.protocols :as model]
            [lab.core [plugin :as plugin]
                      [lang :as lang]
                      [keymap :as km]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Basic Insertion Commands

(def delimiters {\( \), \[ \], \{ \}, \" \"})

(def ignore? #{:net.cgrand.parsley/unfinished
               :net.cgrand.parsley/unexpected
               :string :comment :char :regex})

(defn- balance-delimiter [app e]
  (let [editor    (:source e)
        opening   (:char e)
        closing   (delimiters opening)
        offset    (ui/caret-position editor)
        root-loc  (-> @(ui/attr editor :doc) lang/parse-tree lang/code-zip)
        [loc pos] (lang/location root-loc offset)
        tag       (lang/location-tag loc)
        s         (str opening (when-not (ignore? tag) closing))]
    (model/insert editor offset s)
    (ui/caret-position editor (inc offset))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Movement & Navigation

(defn- move [app e movement]
  (let [editor  (:source e)
        pos     (ui/caret-position editor)
        doc     (ui/attr editor :doc)
        tree    (lang/code-zip (lang/parse-tree @doc))
        [loc i] (lang/location tree pos)
        nxt     (movement loc)
        pos     (when nxt (lang/offset nxt))]
  (when pos
    (ui/action (ui/caret-position editor pos)))))

(defn move-back [loc]
  (if (or (zip/right loc) (nil? (zip/left loc)))
    (-> loc zip/up zip/left)
    (-> loc zip/left)))

(defn- backward [app e]
  (move app e move-back))

(defn- forward [app e]
  (move app e #(-> % zip/up zip/right)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Movement & Navigation

(defn- wrap-around
  "Looks for the location under the current caret position,
finds the leftmost sibling in order to get the offset of the
current form and add a closing and opening parentheses around
it.

(foo |bar baz)
(foo (|bar) baz)"
  [app e]
  (let [editor  (:source e)
        pos     (ui/caret-position editor)
        doc     (ui/attr editor :doc)
        tree    (lang/code-zip (lang/parse-tree @doc))
        [loc i] (lang/location tree pos)]
    (when loc
      (let [parent  (zip/up loc)
            left    (-> loc zip/leftmost)
            len     (-> parent zip/node lang/node-length)
            i       (if (= left loc) i (lang/offset left))]
        (when-not (lang/whitespace? parent)
          (ui/action
            (model/insert editor (+ i len) ")")
            (model/insert editor i "(")))))))

(defn- list-parent
  "Returns the first location that contains a parent :list node."
  [loc]
  (if (or (nil? loc) (= :list (-> loc zip/node :tag)))
    loc
    (recur (zip/up loc))))

(defn- splice-sexp
  "Looks for the location under the current caret position,
then gets the first parent list it finds and removes the wrapping
parentheses by deleting and inserting the modified substring.

(foo (bar| baz) quux)
(foo bar| baz quux)"
  [app e]
  (let [editor  (:source e)
        pos     (ui/caret-position editor)
        doc     (ui/attr editor :doc)
        tree    (lang/code-zip (lang/parse-tree @doc))
        [loc i] (lang/location tree pos)
        parent  (list-parent loc)]
    (when parent
      (ui/action
        (let [[start end] (lang/limits parent)
              s   (model/substring editor start end)]
          (model/delete editor start end)
          (model/insert editor start (->> s rest butlast (apply str))))))))

(defn- splice-sexp-killing
  "Looks for the location in the current caret position.
If the node in the location is a list then it looks for the next list
up in the tree, otherwise takes the first list as the parent. Then
gets the offset limits for the location and the list's location, replacing
the parent's text for the string returned by f.
f is a function that takes the editor, the limits for the location and
the limits for the parent list, returning the string that will replace
the parent's list text."
  [app e f]
  (let [editor  (:source e)
        pos     (ui/caret-position editor)
        doc     (ui/attr editor :doc)
        tree    (lang/code-zip (lang/parse-tree @doc))
        [loc i] (lang/location tree pos)
        tag     (lang/location-tag loc)
        [loc parent]
                (if (= :list tag)
                  [(-> loc list-parent) (-> loc list-parent zip/up list-parent)]
                  [loc (-> loc list-parent)])]
    (when parent
      (ui/action
        (let [[start end]   (lang/limits loc)
              [pstart pend] (lang/limits parent)
              s             (f editor [start end] [pstart pend])]
          (model/delete editor pstart pend)
          (model/insert editor pstart s))))))

(defn- splice-sexp-killing-backward
  "(foo (let ((x 5)) |(sqrt n)) bar)
   (foo |(sqrt n) bar)"
  [app e]
  (splice-sexp-killing app e
    (fn [editor [start end] [pstart pend]]
      (->> (model/substring editor start pend)
        butlast
        (apply str)))))

(defn- splice-sexp-killing-forward [app e]
  "(a (b c| d e) f)
   (a b c| f)"
  [app e]
  (splice-sexp-killing app e
    (fn [editor [start end] [pstart pend]]
      (->> (model/substring editor pstart start)
        rest
        (apply str)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Keymap

(def ^:private keymaps
  [(km/keymap 'lab.plugin.paredit
    :lang :clojure
    {:fn ::balance-delimiter :keystroke "(" :name "Balance parenthesis"}
    {:fn ::balance-delimiter :keystroke "(" :name "Balance parenthesis"}
    {:fn ::balance-delimiter :keystroke "{" :name "Balance curly brackets"}
    {:fn ::balance-delimiter :keystroke "[" :name "Balance square brackets"}
    {:fn ::balance-delimiter :keystroke "\"" :name "Balance double quotes"}
    ;; Movement & Navigation
    {:fn ::backward :keystroke "ctrl alt left" :name "Backward"}
    {:fn ::forward :keystroke "ctrl alt right" :name "Forward"}
    ;; Depth-Changing Commands
    {:fn ::wrap-around :keystroke "alt (" :name "Wrap around"}
    {:fn ::splice-sexp :keystroke "alt s" :name "Splice sexp"}
    {:fn ::splice-sexp-killing-backward :keystroke "alt up" :name "Splice sexp"}
    {:fn ::splice-sexp-killing-forward :keystroke "alt down" :name "Splice sexp"})])

(plugin/defplugin lab.plugin.paredit
  :keymaps keymaps)
