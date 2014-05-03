(ns lab.plugin.editor.paredit
  (:require [clojure.zip :as zip]
            [clojure.core.match :refer [match]]
            [lab.ui.core :as ui]
            [lab.util :refer [timeout-channel find-limits find-char]]
            [lab.model.document :as doc]
            [lab.model.protocols :as model]
            [lab.core [plugin :as plugin]
                      [lang :as lang]
                      [keymap :as km]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Util

(defn- editor-info
  "Takes a key event and returns a map with the computed
information necessary for most paredit commands."
  [{:keys [source char] :as e}]
  (let [tree (lang/code-zip (lang/parse-tree @(ui/attr source :doc)))
        pos  (ui/caret-position source)]
    {:editor   source
     :pos      pos
     :ch       char
     :doc      (ui/attr source :doc)
     :tree     tree
     :location (lang/location tree pos)}))

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

(def delimiter? (reduce into #{} delimiters))

(def ignore? #{:net.cgrand.parsley/unfinished
               :net.cgrand.parsley/unexpected
               :string :comment :char :regex})

(defn open-delimiter
  "Opens a delimiter and inserts the closing one.

(a b |c d)
(a b (|) c d)
(foo \"bar |baz\" quux)
(foo \"bar |(baz\" quux)"
  [e]
  (let [{:keys [editor ch tree pos location]}
                 (editor-info e)
        [loc i]  location
        closing  (delimiters ch)
        tag      (lang/location-tag loc)
        s        (str ch
                      (when-not (and (ignore? tag) (not= i pos))
                        closing))]
    (model/insert editor pos s)
    (ui/caret-position editor (inc pos))))

(defn close-delimiter
  "Moves the caret to the closest closing delimiter
and removes all whitespace between the last element
and the closing delimiter.

(a b |c )
(a b c)|
; Hello,| world!
; Hello,)| world!"
  [e]
  (let [{:keys [editor ch tree pos location]}
                 (editor-info e)
        [loc i]  location
        tag      (lang/location-tag loc)]
    (if (and (ignore? tag) (not= \" ch))
      (ui/action (model/insert editor pos (str ch)))
      (let [parent  (or (delim-parent loc)
                        (as-> (zip/up loc) p
                          (when (= :string (lang/location-tag p)) p)))
            [start end] (when parent (lang/limits parent))
            end-loc (when parent (-> parent zip/down zip/rightmost zip/left))
            [wstart wend] (when (lang/whitespace? end-loc) (lang/limits end-loc))
            delim   (when end (get (model/text editor) (dec end)))]
        ;; When there's a parent with delimiters and the
        ;; char inserted is the closing delim.
        (when (and start (= delim ch))
          (ui/action
            (when wstart (model/delete editor wstart wend))
            (ui/caret-position editor (or (and wstart (inc wstart)) end))))))))


(defn- location-index
  "Returns the index of the location in the parent's children vector"
  [loc]
  (loop [loc loc
         i 0]
    (if-not loc
      i
      (recur (zip/left loc)
             (if-not (or (lang/whitespace? loc) (lang/loc-string? loc))
               (inc i) i)))))

(defn- indentation
  "Figures out the indentation for loc based on its parent
and the position relative to the sibilings."
  [editor ploc loc]
  (let [s     (model/text editor)
        tag   (lang/location-tag ploc)
        delim (lang/offset ploc)
        snd-loc (-> ploc zip/down (adjacent zip/right) (adjacent zip/right))
        snd   (lang/offset snd-loc)
        start (inc (or (find-char s delim #{\newline} dec) 0))
        index (location-index loc)]
    (match [tag index]
      [(:or :list :fn) 1] (inc (- delim start))
      [(:or :list :fn) 2] (+ (- delim start) 2)
      [(:or :list :fn) _] (- snd start)
      [(:or :vector :set :map) _] (inc (- delim start)))))

(defn- format-code [e]
  (let [{:keys [editor ch tree pos location]}
                 (editor-info e)
        [loc i]  location
        delim?  (and (lang/loc-string? loc) (-> loc zip/left nil? not))
        loc     (if delim? loc (zip/up loc))
        loc     (if (lang/whitespace? loc)
                  (or (adjacent loc zip/right)
                      (-> loc zip/down zip/rightmost))
                  loc)
        ploc    (delim-parent loc)
        tag     (lang/location-tag ploc)]
    (when (and ploc (not (ignore? tag)))
      (let [indent(indentation editor ploc loc)
            spc   (apply str (repeat indent " "))
            [wstart wend] (when (-> loc zip/left lang/whitespace?) (-> loc zip/left lang/limits))]
        (if wstart
          (let [ws (->> (model/substring editor wstart wend)
                     (remove #{\space})
                     (apply str))]
            (model/delete editor wstart wend)
            (model/insert editor wstart ws)
            (model/insert editor (+ wstart (count ws)) spc))
          (model/insert editor pos spc))))))

(defn insert-newline
  "Inserts a newline and formats the following lines.

  (let ((n frobbotz)) | (display (+ n 1)
    port))
  (let ((n frobbotz))
    |(display (+ n 1)
              port))"
  [e]
  (let [editor  (:source e)]
    (ui/action
      (model/insert editor (ui/caret-position editor) "\n")
      (format-code e))))

(defn close-delimiter-and-newline
  "Closes a delimiter and inserts a newline.

  (defun f (x|  ))
  (defun f (x)
    |)

  ; (Foo.|
  ; (Foo.)|"
  [e]
  (close-delimiter e)
  (insert-newline e))

(defn comment-dwin [e]
  (prn ::comment-dwin))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Deleting and killing

(defn- delete-pos
  "Deletes the character at pos from the contents of the
  editor, taking into account if the character is a delimiter 
  or not and the direction the caret should move if it is."
  [editor pos [loc i] dir]
  (let [txt  (model/text editor)
        len  (model/length editor)
        ch   (get txt pos)
        ploc (zip/up loc)
        tag  (lang/location-tag loc)
        llen (lang/location-length loc)]
    (when (<= 0 pos (dec len))
      (cond
        (or (not (delimiter? ch))
            (and (ignore? tag) (< i pos (+ i llen))))
          (model/delete editor pos (inc pos))
        (and (not= \" ch)
             (delimiter? ch)
             (-> ploc zip/children count (<= 2)))
          (let [start (lang/offset ploc)
                len   (lang/location-length ploc)]
           (model/delete editor start (+ start len)))
        (and (= \" ch)
             (lang/loc-string? loc)
             (= 2 (lang/location-length loc)))
          (model/delete editor i (+ i llen))
        :else
          (ui/caret-position editor (dir pos))))))

(defn- delete-selection
  "Deletes the selection of characters but not the delimiters."
  [editor start end]
  (let [txt (model/text editor)
        sel (subs txt start end)]
    (model/delete editor start end)
    (model/insert editor start (->> sel (remove (comp not delimiter?)) (apply str)))))

(defn- forward-delete  
  "(quu|x \"zot\")
  (quu| \"zot\")

  (quux |\"zot\")
  (quux \"|zot\")
  (quux \"|ot\")

  (foo (|) bar)
  (foo | bar)

  |(foo bar)
  (|foo bar)"
  [e]
  (let [{:keys [editor ch tree pos location]}
                 (editor-info e)
        [s e]    (ui/selection editor)]
    (if (= s e)
      (delete-pos editor pos location inc)
      (delete-selection editor s e))))

(defn- backward-delete
  "(\"zot\" q|uux)
  (\"zot\" |uux)

  (\"zot\"| quux)
  (\"zot|\" quux)
  (\"zo|\" quux)

  (foo (|) bar)
  (foo | bar)

  (foo bar)|
  (foo bar|)"
  [e]
  (let [{:keys [editor ch tree pos]}
                 (editor-info e)
        location (lang/location tree (dec pos)) ;; get the location for the prev position
        [s e]    (ui/selection editor)]
    (if (= s e)
      (delete-pos editor (dec pos) location identity)
      (delete-selection editor s e))))

(defn- kill
  "(foo bar)|     ; Useless comment!
  (foo bar)|

  (|foo bar)     ; Useful comment!
  (|)     ; Useful comment!

  |(foo bar)     ; Useless line!
  |
  (foo \"|bar baz\"
       quux)
  (foo \"|\"
       quux)"
  [e]
  (let [{:keys [editor ch tree pos location]}
                (editor-info e)
        [loc i] location
        ploc    (delim-parent loc)
        tag     (lang/location-tag loc)]
    (cond
      ploc
        (let [fst   (zip/down ploc)
              start (lang/offset (zip/right fst))
              end   (-> fst zip/rightmost lang/offset)]
          (model/delete editor start end))
      (= :string tag)
        (model/delete editor
                      (inc i)
                      (+ i (dec (lang/location-length loc))))
      :else
        (when-let [end (find-char (model/text editor) pos #{\newline} inc)]
          (model/delete editor pos end)))))

(def ^:private word?
  (comp (partial re-find #"[A-Za-z_0-9¡!$%&*+\-\./<=>¿?]")
        str))

(def ^:private space? #{\space})

(defn- kill-word
  [e dir]
  (let [{:keys [editor ch tree pos location]}
               (editor-info e)
        txt    (model/text editor)
        start  (find-char txt pos word? dir)
        end    (when start (find-char txt (dir start) (comp not word?) dir))]
    (when-let [[start end] (and start end
                                (map #(if (= dir dec) (inc %) %)         ; if backwards then increment limits.
                                     [(min start end) (max start end)]))]
      (model/delete editor (or (when (and (not= dir dec)
                                          (space? (get txt (dec start))))
                                 (inc (find-char txt (dec start) (comp not space?) dec)))
                               start)
                           (or (when (and (= dir dec)
                                          (space? (get txt end)))
                                 (find-char txt end (comp not space?) inc))
                               end)))))

(defn- forward-kill-word
  "|(foo bar)    ; baz
  (| bar)    ; baz
  (|)    ; baz
  ()    ;|

  ;;;| Frobnicate
  (defun frobnicate ...)
  ;;;|
  (defun frobnicate ...)
  ;;;
  (| frobnicate ...)"
  [e]
  (kill-word e inc))

(defn- backward-kill-word
  "(foo bar)    ; baz
  (quux)|

  (foo bar)    ; baz
  (|)

  (foo bar)    ; |
  ()

  (foo |)    ; 
  ()

  (|)    ; 
  ()"
  [e]
  (kill-word e dec))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Movement & Navigation

(defn- move [e movement]
  (let [{:keys [editor ch tree pos location]}
                 (editor-info e)
        [loc i]  location
        nxt     (movement loc)
        pos     (when nxt (lang/offset nxt))]
  (when pos
    (ui/action (ui/caret-position editor pos)))))

(defn- move-back [loc]
  (if (or (zip/right loc) (nil? (zip/left loc)))
    (-> loc zip/up zip/left)
    (-> loc zip/left)))

(defn backward
  "Moves the caret to the start of the current
form or the end of the next one.

(foo (bar baz)| quux)
(foo |(bar baz) quux)
(|(foo) bar)
|((foo) bar)"
  [e]
  (move e move-back))

(defn forward
  "Moves the caret to the end of the current
form or the start of the next one.

(foo |(bar baz) quux)
(foo (bar baz)| quux)
(foo (bar baz)|)
(foo (bar baz))|"
  [e]
  (move e #(-> % zip/up zip/right)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Depth-Changing Commands

(defn- wrap-around
  "Looks for the location under the current caret position,
finds the leftmost sibling in order to get the offset of the
current form and add a closing and opening parentheses around
it.

(foo |bar baz)
(foo (|bar) baz)"
  [e]
  (let [{:keys [editor ch tree pos location]}
                 (editor-info e)
        [loc i]  location]
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
  [e f]
  (let [{:keys [editor ch tree pos location]}
                 (editor-info e)
        [loc i]  location
        tag     (lang/location-tag loc)
        [loc parent]
                (if (= :list tag)
                  [(-> loc delim-parent) (-> loc delim-parent zip/up delim-parent)]
                  [(zip/up loc) (-> loc delim-parent)])]
    (when (and parent
               #_(not (lang/whitespace? loc)) ; commented out so it doesn't matter where 
                                              ; inside a form you are when splicing.
               )
      (ui/action
        ; TODO: take into account delimiters like #{ or #(
        (let [[start end]   (lang/limits loc)
              [pstart pend] (lang/limits parent)
              s             (f editor [start end] [pstart pend])]
          (model/delete editor pstart pend)
          (model/insert editor pstart s)
          (ui/caret-position editor (dec pos)))))))

(defn splice-sexp
  "Looks for the location under the current caret position,
then gets the first parent list it finds and removes the wrapping
parentheses by deleting and inserting the modified substring.

(foo (bar| baz) quux)
(foo bar| baz quux)"
  [e]
  (splice-sexp-killing e
    (fn [editor _ [pstart pend]]
      (->> (model/substring editor pstart pend)
        rest
        butlast
        (apply str)))))

(defn splice-sexp-killing-backward
  "(foo (let ((x 5)) |(sqrt n)) bar)
   (foo |(sqrt n) bar)"
  [e]
  (splice-sexp-killing e
    (fn [editor [start end] [pstart pend]]
      (->> (model/substring editor start pend)
        butlast
        (apply str)))))

(defn splice-sexp-killing-forward
  "(a (b c| d e) f)
   (a b c| f)"
  [e]
  (splice-sexp-killing e
    (fn [editor [start end] [pstart pend]]
      (->> (model/substring editor pstart start)
        rest
        (apply str)))))

(defn raise-sexp
  "(dynamic-wind in (lambda [] |body) out)
(dynamic-wind in |body out)
|body"
  [e]
  (splice-sexp-killing e
    (fn [editor [start end] [pstart pend]]
      (model/substring editor start end))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Barfage & Slurpage

(defn- slurp-sexp
  [e dir dirmost f]
  (let [{:keys [editor ch tree pos location]}
                 (editor-info e)
        [loc i]  location
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

(defn forward-slurp-sexp
  "(foo (bar |baz) quux zot)
(foo (bar |baz quux) zot)

(a b ((c| d)) e f)
(a b ((c| d) e) f)
(a b ((c| d e)) f)"
  [e]
  (slurp-sexp e zip/right zip/rightmost
              (fn [editor pos [pstart pend] [start end] delim]
                (model/delete editor (dec pend) pend)
                (model/insert editor (dec end) delim)
                (ui/caret-position editor pos))))

(defn backward-slurp-sexp
  "(foo bar (baz| quux) zot)
(foo (bar| baz quux) zot)

(a b ((c| d)) e f)
(a (b (c| d) e) f)"
  [e]
  (slurp-sexp e zip/left zip/leftmost
              (fn [editor pos [pstart pend] [start end] delim]
                (model/delete editor pstart (inc pstart))
                (model/insert editor start delim)
                (ui/caret-position editor pos))))

(defn barf-sexp
  [e dir dirmost f]
  (let [{:keys [editor ch tree pos location]}
                 (editor-info e)
        [loc i]  location
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

(defn forward-barf-sexp
  "(foo (bar |baz quux) zot)
(foo (bar |baz) quux zot)"
  [e]
  (barf-sexp e zip/left zip/rightmost
             (fn [editor pos [pstart pend] [start end] delim]
               (when (<= pos end)
                 (model/delete editor (dec pend) pend)
                 (model/insert editor end delim)
                 (ui/caret-position editor pos)))))

(defn backward-barf-sexp
  "(foo (bar baz |quux) zot)
(foo bar (baz |quux) zot)"
  [e]
  (barf-sexp e zip/right zip/leftmost
             (fn [editor pos [pstart pend] [start end] delim]
               (when (>= pos (dec start))
                 (model/delete editor pstart (inc pstart))
                 (model/insert editor (dec start) delim)
                 (ui/caret-position editor pos)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Keymap

(def ^:private keymaps
  [(km/keymap (ns-name *ns*)
    :local
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
    ;; Deleting and killing
    {:fn ::forward-delete :keystroke "delete" :name "Delete forward"}
    {:fn ::backward-delete :keystroke "back_space" :name "Delete backward"}
    {:fn ::kill :keystroke "ctrl k" :name "Kill"}
    {:fn ::forward-kill-word :keystroke "alt d" :name "Forward kill word"}
    {:fn ::backward-kill-word :keystroke "alt back_space" :name "Backward kill word"}
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

(plugin/defplugin (ns-name *ns*)
  :type    :local
  :keymaps keymaps)
