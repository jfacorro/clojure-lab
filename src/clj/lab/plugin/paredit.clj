(ns lab.plugin.paredit
  (:require [clojure.zip :as zip]
            [lab.ui.core :as ui]
            [lab.util :refer [timeout-channel find-limits]]
            [lab.model.document :as doc]
            [lab.model.protocols :as model]
            [lab.core [plugin :as plugin]
                      [lang :as lang]
                      [keymap :as km]]))

(def delimiters
  {\( \)
   \[ \]
   \{ \}
   \" \"})

(def ignore? #{:net.cgrand.parsley/unfinished
               :net.cgrand.parsley/unexpected
               :string :comment :char :regex})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Basic Insertion Commands

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

)])

(plugin/defplugin lab.plugin.paredit
  :keymaps keymaps)
