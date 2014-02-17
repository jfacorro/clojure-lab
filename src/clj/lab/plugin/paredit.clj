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
it."
  [app e]
  (let [editor  (:source e)
        pos     (ui/caret-position editor)
        doc     (ui/attr editor :doc)
        tree    (lang/code-zip (lang/parse-tree @doc))
        [loc i] (lang/location tree pos)
        parent  (zip/up loc)
        left    (-> loc zip/leftmost)
        len     (-> parent zip/node lang/node-length)
        i       (if (= left loc) i (lang/offset left))]
    (when-not (lang/whitespace? parent)
      (ui/action
        (model/insert editor (+ i len) ")")
        (model/insert editor i "(")))))

(defn- list-parent
  "Returns the first location that contains a parent :list node."
  [loc]
  (if (or (nil? loc) (= :list (-> loc zip/node :tag)))
    loc
    (recur (zip/up loc))))

(defn- splice-sexp
  "Looks for the location under the current caret position,
then gets the first parent list it finds and removes the wrapping
parentheses by deleting and inserting the modified substring."
  [app e]
  (let [editor  (:source e)
        pos     (ui/caret-position editor)
        doc     (ui/attr editor :doc)
        tree    (lang/code-zip (lang/parse-tree @doc))
        [loc i] (lang/location tree pos)
        parent  (list-parent loc)]
    (when parent
      (ui/action
        (let [i   (lang/offset parent)
              len (-> parent zip/node lang/node-length)
              s   (model/substring editor i (+ i len))]
          (model/delete editor i (+ i len))
          (model/insert editor i (->> s rest butlast (apply str))))))))

(defn- splice-sexp-killing-backward [app e]
  (prn ::splice-sexp-killing-backward))

(defn- splice-sexp-killing-forward [app e]
  (prn ::splice-sexp-killing-forward))

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
