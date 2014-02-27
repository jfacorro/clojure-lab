(ns lab.plugin.paredit
  (:require [clojure.zip :as zip]
            [clojure.core.match :refer [match]]
            [lab.ui.core :as ui]
            [lab.util :refer [timeout-channel find-limits]]
            [lab.model.document :as doc]
            [lab.model.protocols :as model]
            [lab.core [plugin :as plugin]
                      [lang :as lang]
                      [keymap :as km]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Util

(defn- list-parent
  "Returns the first location that contains a parent :list node
or nil if there's not parent list."
  [loc]
  (lang/select-location loc zip/up
                   #(or (nil? %)
                        (= :list (-> % zip/node :tag)))))

(defn- delim-parent
  "Returns the first location that contains a parent :list node."
  [loc]
  (lang/select-location (zip/up loc) zip/up
                   #(or (nil? %)
                        (#{:list :vector :map :set :fn} (-> % zip/node :tag)))))

(defn- adjacent
  "Returns the first sibling location in the direction
specified that's not a whitespace or that contains a 
string node."
  [loc dir]
  (lang/select-location (dir loc)
    dir
    #(not (or (lang/whitespace? %)
              (lang/loc-string? %)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Basic Insertion Commands

(def delimiters {\( \), \[ \], \{ \}, \" \"})

(def ignore? #{:net.cgrand.parsley/unfinished
               :net.cgrand.parsley/unexpected
               :string :comment :char :regex})

(defn- open-delimiter
  "Opens a delimiter and inserts the closing one.

(a b |c d)
(a b (|) c d)
(foo \"bar |baz\" quux)
(foo \"bar |(baz\" quux)"
  [app e]
  (let [editor    (:source e)
        opening   (:char e)
        closing   (delimiters opening)
        offset    (ui/caret-position editor)
        root-loc  (-> @(ui/attr editor :doc) lang/parse-tree lang/code-zip)
        [loc pos] (lang/location root-loc offset)
        tag       (lang/location-tag loc)
        s         (str opening
                       (when-not (and (ignore? tag) (not= pos offset))
                         closing))]
    (ui/action
      (model/insert editor offset s)
      (ui/caret-position editor (inc offset)))))

(defn- close-delimiter
  "Moves the caret to the closest closing delimiter
and removes all whitespace between the last element
and the closing delimiter.

(a b |c )
(a b c)|
; Hello,| world!
; Hello,)| world!"
  [app e]
  (let [editor (:source e)
        pos    (ui/caret-position editor)
        ch     (:char e)
        doc    (ui/attr editor :doc)
        tree   (lang/code-zip (lang/parse-tree @doc))
        [loc i](lang/location tree pos)
        tag    (lang/location-tag loc)]
    (if (and (ignore? tag) (not= \" ch))
      (ui/action (model/insert editor pos (str ch)))
      (let [parent  (or (delim-parent loc)
                        (as-> (zip/up loc) p
                          (and (= :string (lang/location-tag p)) p) ))
            [start end] (and parent (lang/limits parent))
            end-loc (and parent (-> parent zip/down zip/rightmost zip/left))
            [wstart wend] (when (lang/whitespace? end-loc) (lang/limits end-loc))
            delim   (when end (get (model/text editor) (dec end)))]
        ;; When there's a parent with delimiters and the
        ;; char inserted is the closing delim.
        (when (and start (= delim ch))
          (ui/action
            (when wstart (model/delete editor wstart wend))
            (ui/caret-position editor (or (and wstart (inc wstart)) end))))))))

(defn- find-char
  "Finds the next char in s for which pred is true,
  starting to look from position cur, in the direction 
  specified by dt (1 or -1)."
  [s cur pred dt]
  (cond (or (neg? cur) (<= (.length s) cur)) nil
        (pred (get s cur)) cur
        :else (recur s (+ cur dt) pred dt)))

(defn- indentation
  [editor ploc loc]
  (let [s     (model/text editor)
        tag   (lang/location-tag ploc)
        delim (lang/offset ploc)
        snd-loc (-> ploc zip/down (adjacent zip/right) (adjacent zip/right))
        snd   (lang/offset snd-loc)
        start (inc (or (find-char s delim #{\newline} -1) 0))
        index (location-index loc)]
    (match [tag index]
      [:list 1] (inc (- delim start))
      [:list 2] (+ (- delim start) 2)
      [:list _] (- snd start)
      [(:or :vector :set :map) _] (inc (- delim start)))))

(defn- location-index [loc]
  (loop [loc loc
         i 0]
    (if-not loc
      i
      (recur (zip/left loc)
             (if-not (or (lang/whitespace? loc) (lang/loc-string? loc))
               (inc i) i)))))

(defn- format-code [app e]
  (let [editor  (:source e)
        doc     (ui/attr editor :doc)
        tree    (lang/code-zip (lang/parse-tree @doc))
        pos     (ui/caret-position editor)
        [loc i] (lang/location tree pos)
        loc     (if (lang/whitespace? loc)
                  (or (adjacent (zip/up loc) zip/right)
                      (adjacent (zip/up loc) zip/left))
                  loc)
        ploc    (delim-parent loc)
        tag     (lang/location-tag ploc)]
    (when (and ploc (not (ignore? tag)))
      (let [indent(indentation editor ploc loc)
            spc   (apply str (repeat indent " "))]
        (model/insert editor pos spc)))))

(defn- insert-newline
  "Inserts a newline and formats the following lines.

(let ((n frobbotz)) | (display (+ n 1)
  port))
(let ((n frobbotz))
  |(display (+ n 1)
            port))"
  [app e]
  (let [editor  (:source e)]
    (ui/action
      (model/insert editor (ui/caret-position editor) "\n")
      (format-code app e))))

(defn- close-delimiter-and-newline
  "Closes a delimiter and inserts a newline.

(defun f (x|  ))
(defun f (x)
  |)
; (Foo.|
; (Foo.)|"
  [app e]
  (close-delimiter app e)
  (insert-newline app e))

(defn- comment-dwin [app e]
  (prn ::comment-dwin))

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

(defn- backward
  "Moves the caret to the start of the current
form or the end of the next one.

(foo (bar baz)| quux)
(foo |(bar baz) quux)
(|(foo) bar)
|((foo) bar)"
  [app e]
  (move app e move-back))

(defn- forward
  "Moves the caret to the end of the current 
form or the start of the next one.

(foo |(bar baz) quux)
(foo (bar baz)| quux)
(foo (bar baz)|)
(foo (bar baz))|"
  [app e]
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

(defn- slurp-sexp
  [editor dir dirmost f]
  (let [pos     (ui/caret-position editor)
        doc     (ui/attr editor :doc)
        tree    (lang/code-zip (lang/parse-tree @doc))
        [loc i] (lang/location tree pos)
        [parent next-loc]
                (loop [parent   (delim-parent loc)
                       next-loc (adjacent parent dir)]
                  (if next-loc
                    [parent next-loc]
                    (when-let [parent (-> parent zip/up delim-parent)]
                      (recur parent (adjacent parent dir)))))]
    (when (and parent next-loc)
      (let [[pstart pend] (lang/limits parent)
            delim         (-> parent zip/down dirmost zip/node)
            [start end]   (lang/limits next-loc)]
        (f editor pos [pstart pend] [start end] delim)))))

(defn- forward-slurp-sexp
  "(foo (bar |baz) quux zot)
(foo (bar |baz quux) zot)

(a b ((c| d)) e f)
(a b ((c| d) e) f)
(a b ((c| d e)) f)"
  [app e]
  (slurp-sexp (:source e) zip/right zip/rightmost
              (fn [editor pos [pstart pend] [start end] delim]
                (model/delete editor (dec pend) pend)
                (model/insert editor (dec end) delim)
                (ui/caret-position editor pos))))

(defn- backward-slurp-sexp
  "(foo bar (baz| quux) zot)
(foo (bar| baz quux) zot)

(a b ((c| d)) e f)
(a (b (c| d) e) f)"
  [app e]
  (slurp-sexp (:source e) zip/left zip/leftmost
              (fn [editor pos [pstart pend] [start end] delim]
                (model/delete editor pstart (inc pstart))
                (model/insert editor start delim)
                (ui/caret-position editor pos))))

(defn- barf-sexp
  [editor dir dirmost f]
  (let [pos      (ui/caret-position editor)
        doc      (ui/attr editor :doc)
        tree     (lang/code-zip (lang/parse-tree @doc))
        [loc i]  (lang/location tree pos)
        parent   (delim-parent loc)
        next-loc (when parent
                   (-> parent zip/down dirmost
                     (adjacent dir)
                     (adjacent dir)))]
    (when (and parent next-loc)
      (let [plims (lang/limits parent)
            delim (-> parent zip/down dirmost zip/node)
            lims  (lang/limits next-loc)]
        (f editor pos plims lims delim)))))

(defn- forward-barf-sexp
  "(foo (bar |baz quux) zot)
(foo (bar |baz) quux zot)"
  [app e]
  (barf-sexp (:source e) zip/left zip/rightmost
             (fn [editor pos [pstart pend] [start end] delim]
               (when (<= pos end)
                 (model/delete editor (dec pend) pend)
                 (model/insert editor end delim)
                 (ui/caret-position editor pos)))))

(defn- backward-barf-sexp
  "(foo (bar baz |quux) zot)
(foo bar (baz |quux) zot)"
  [app e]
  (barf-sexp (:source e) zip/right zip/leftmost
             (fn [editor pos [pstart pend] [start end] delim]
               (when (>= pos (dec start))
                 (model/delete editor pstart (inc pstart))
                 (model/insert editor (dec start) delim)
                 (ui/caret-position editor pos)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Keymap

(def ^:private keymaps
  [(km/keymap 'lab.plugin.paredit
    :lang :clojure
    ;; Basic Insertion Commands
    {:fn ::open-delimiter :keystroke "(" :name "Open round"}
    {:fn ::close-delimiter :keystroke ")" :name "Close round"}
    {:fn ::close-delimiter-and-newline :keystroke "alt )" :name "Close round and newline"}
    {:fn ::open-delimiter :keystroke "{" :name "Open curly brackets"}
    {:fn ::close-delimiter :keystroke "}" :name "Close curly brackets"}
    {:fn ::close-delimiter-and-newline :keystroke "alt }" :name "Close curly brackets and newline"}
    {:fn ::open-delimiter :keystroke "[" :name "Open square brackets"}
    {:fn ::close-delimiter :keystroke "]" :name "Close square brackets"}
    {:fn ::close-delimiter-and-newline :keystroke "alt ]" :name "Close square brackets and newline"}
    {:fn ::open-delimiter :keystroke "\"" :name "Open double quotes"}
    {:fn ::close-delimiter :keystroke "alt \"" :name "Close double quotes"}
    {:fn ::comment-dwin :keystroke "alt ;" :name "Comment dwim"}
    {:fn ::insert-newline :keystroke "ctrl j" :name "Newline"}
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
