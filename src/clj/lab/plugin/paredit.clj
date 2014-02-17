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
;; Util

(defn- select-location [loc dir p]
  (if (p loc)
    loc
    (recur (dir loc) dir p)))

(defn- list-parent
  "Returns the first location that contains a parent :list node."
  [loc]
  (select-location loc zip/up
                   #(or (nil? %)
                        (= :list (-> % zip/node :tag)))))

(defn- coll-parent
  "Returns the first location that contains a parent :list node."
  [loc]
  (select-location loc zip/up
                   #(or (nil? %)
                        (#{:list :vector :map :set :fn} (-> % zip/node :tag)))))

(defn- adjacent-loc [loc dir]
  (select-location (dir loc)
    dir
    #(and (not (lang/whitespace? %))
          (not (lang/loc-string? %)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Basic Insertion Commands

(def delimiters {\( \), \[ \], \{ \}, \" \"})

(def ignore? #{:net.cgrand.parsley/unfinished
               :net.cgrand.parsley/unexpected
               :string :comment :char :regex})

(defn- open-delimiter [app e]
  (let [editor    (:source e)
        opening   (:char e)
        closing   (delimiters opening)
        offset    (ui/caret-position editor)
        root-loc  (-> @(ui/attr editor :doc) lang/parse-tree lang/code-zip)
        [loc pos] (lang/location root-loc offset)
        tag       (lang/location-tag loc)
        s         (str opening (when-not (ignore? tag) closing))]
    (ui/action
      (model/insert editor offset s)
      (ui/caret-position editor (inc offset)))))

(defn- close-delimiter [app e]
  (let [editor (:source e)
        pos    (ui/caret-position editor)
        ch     (:char e)
        doc    (ui/attr editor :doc)
        tree   (lang/code-zip (lang/parse-tree @doc))
        [loc i](lang/location tree pos)
        tag    (lang/location-tag loc)]
    (if (ignore? tag)
      (ui/action (model/insert editor pos (str ch)))
      (let [parent (coll-parent loc)
            [start end] (and parent (lang/limits parent))
            delim  (get (model/text editor) (dec end))]
        (when (and start (= delim ch))
          (ui/action (ui/caret-position editor end)))))))

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
;; Depth-Changing Commands

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
                  [(zip/up loc) (-> loc list-parent)])]
    (when (and parent (not (lang/whitespace? loc)))
      (ui/action
        (let [[start end]   (lang/limits loc)
              [pstart pend] (lang/limits parent)
              s             (f editor [start end] [pstart pend])]
          (model/delete editor pstart pend)
          (model/insert editor pstart s)
          (ui/caret-position editor pstart))))))

(defn- splice-sexp
  "Looks for the location under the current caret position,
then gets the first parent list it finds and removes the wrapping
parentheses by deleting and inserting the modified substring.

(foo (bar| baz) quux)
(foo bar| baz quux)"
  [app e]
  (splice-sexp-killing app e
    (fn [editor _ [pstart pend]]
      (->> (model/substring editor pstart pend)
        rest
        butlast
        (apply str)))))

(defn- splice-sexp-killing-backward
  "(foo (let ((x 5)) |(sqrt n)) bar)
   (foo |(sqrt n) bar)"
  [app e]
  (splice-sexp-killing app e
    (fn [editor [start end] [pstart pend]]
      (->> (model/substring editor start pend)
        butlast
        (apply str)))))

(defn- splice-sexp-killing-forward
  "(a (b c| d e) f)
   (a b c| f)"
  [app e]
  (splice-sexp-killing app e
    (fn [editor [start end] [pstart pend]]
      (->> (model/substring editor pstart start)
        rest
        (apply str)))))

(defn- raise-sexp
  "(dynamic-wind in (lambda [] |body) out)
(dynamic-wind in |body out)
|body"
  [app e]
  (splice-sexp-killing app e
    (fn [editor [start end] [pstart pend]]
      (model/substring editor start end))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Barfage & Slurpage

(defn- forward-slurp-sexp
  "(foo (bar |baz) quux zot)
(foo (bar |baz quux) zot)

(a b ((c| d)) e f)
(a b ((c| d) e) f)
(a b ((c| d e)) f)"
  [app e]
  (let [editor  (:source e)
        pos     (ui/caret-position editor)
        doc     (ui/attr editor :doc)
        tree    (lang/code-zip (lang/parse-tree @doc))
        [loc i] (lang/location tree pos)
        [parent next-loc]
                (loop [parent   (list-parent loc)
                       next-loc (adjacent-loc parent zip/right)]
                  (if next-loc
                    [parent next-loc]
                    (when-let [parent (-> parent zip/up list-parent)]
                      (recur parent (adjacent-loc parent zip/right)))))]
    (when (and parent next-loc)
      (let [[pstart pend] (lang/limits parent)
            [start end]   (lang/limits next-loc)]
        (model/delete editor (dec pend) pend)
        (model/insert editor (dec end) ")")
        (ui/caret-position editor pos)))))

(defn- forward-barf-sexp
  "(foo (bar |baz quux) zot)
(foo (bar |baz) quux zot)"
  [app e]
  (let [editor   (:source e)
        pos      (ui/caret-position editor)
        doc      (ui/attr editor :doc)
        tree     (lang/code-zip (lang/parse-tree @doc))
        [loc i]  (lang/location tree pos)
        parent   (list-parent loc)
        next-loc (-> parent zip/down zip/rightmost
                   (adjacent-loc zip/left)
                   (adjacent-loc zip/left))]
    (when (and parent next-loc)
      (let [[pstart pend] (lang/limits parent)
            [start end]   (lang/limits next-loc)]
        (when (< pos end)
          (model/delete editor (dec pend) pend)
          (model/insert editor end ")")
          (ui/caret-position editor pos))))))

(defn- backward-slurp-sexp
  "(foo bar (baz| quux) zot)
(foo (bar| baz quux) zot)

(a b ((c| d)) e f)
(a (b (c| d) e) f)"
  [app e]
  (let [editor  (:source e)
        pos     (ui/caret-position editor)
        doc     (ui/attr editor :doc)
        tree    (lang/code-zip (lang/parse-tree @doc))
        [loc i] (lang/location tree pos)
        [parent next-loc]
                (loop [parent   (list-parent loc)
                       next-loc (adjacent-loc parent zip/left)]
                  (if next-loc
                    [parent next-loc]
                    (when-let [parent (-> parent zip/up list-parent)]
                      (recur parent (adjacent-loc parent zip/left)))))]
    (when (and parent next-loc)
      (let [[pstart pend] (lang/limits parent)
            [start end]   (lang/limits next-loc)]
        (model/delete editor pstart (inc pstart))
        (model/insert editor start "(")
        (ui/caret-position editor pos)))))

(defn- backward-barf-sexp
  "(foo (bar baz |quux) zot)
(foo bar (baz |quux) zot)"
  [app e]
  (let [editor   (:source e)
        pos      (ui/caret-position editor)
        doc      (ui/attr editor :doc)
        tree     (lang/code-zip (lang/parse-tree @doc))
        [loc i]  (lang/location tree pos)
        parent   (list-parent loc)
        next-loc (-> parent zip/down zip/leftmost
                   (adjacent-loc zip/right)
                   (adjacent-loc zip/right))]
    (when (and parent next-loc)
      (let [[pstart pend] (lang/limits parent)
            [start end]   (lang/limits next-loc)]
        (when (> pos (dec start))
          (model/delete editor pstart (inc pstart))
          (model/insert editor (dec start) "(")
          (ui/caret-position editor pos))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Keymap

(def ^:private keymaps
  [(km/keymap 'lab.plugin.paredit
    :lang :clojure
    {:fn ::open-delimiter :keystroke "(" :name "Open round"}
    {:fn ::close-delimiter :keystroke ")" :name "Close round"}
    {:fn ::close-delimiter-and-newline :keystroke "alt )" :name "Close round and newline"}
    {:fn ::open-delimiter :keystroke "{" :name "Balance curly brackets"}
    {:fn ::close-delimiter :keystroke "}" :name "Close curly brackets"}
    {:fn ::open-delimiter :keystroke "[" :name "Balance square brackets"}
    {:fn ::close-delimiter :keystroke "]" :name "Close square brackets"}
    {:fn ::open-delimiter :keystroke "\"" :name "Balance double quotes"}
    ;; Movement & Navigation
    {:fn ::backward :keystroke "ctrl alt b" :name "Backward"}
    {:fn ::forward :keystroke "ctrl alt f" :name "Forward"}
    ;; Depth-Changing Commands
    {:fn ::wrap-around :keystroke "alt (" :name "Wrap around"}
    {:fn ::splice-sexp :keystroke "alt s" :name "Splice sexp"}
    {:fn ::splice-sexp-killing-backward :keystroke "alt up" :name "Splice sexp backward"}
    {:fn ::splice-sexp-killing-forward :keystroke "alt down" :name "Splice sexp forward"}
    {:fn ::raise-sexp :keystroke "alt r" :name "Raise sexp"}
    ;; Barfage & Slurpage
    {:fn ::forward-slurp-sexp :keystroke "ctrl right" :name "forward-slurp-sexp"}
    {:fn ::forward-barf-sexp :keystroke "ctrl left" :name "forward-barf-sexp"}
    {:fn ::backward-slurp-sexp :keystroke "ctrl alt left" :name "backward-slurp-sexp"}
    {:fn ::backward-barf-sexp :keystroke "ctrl alt right" :name "backward-barf-sexp"})])

(plugin/defplugin lab.plugin.paredit
  :keymaps keymaps)
