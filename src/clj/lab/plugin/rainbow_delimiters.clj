(ns lab.plugin.rainbow-delimiters
  (:require [clojure.zip :as zip]
            [lab.ui.core :as ui]
            [lab.util :refer [timeout-channel find-limits]]
            [lab.model.document :as doc]
            [lab.model.protocols :as model]
            [lab.core [plugin :as plugin]
                      [lang :as lang]]))

(def rainbow-styles
  {1 {:color 0xFF2244}
   2 {:color 0xFF7F00}
   3 {:color 0xFFFF00}
   4 {:color 0x00FF00}
   5 {:color 0x8BFFFF}
   6 {:color 0x0000FF}
   7 {:color 0x8B00FF}})

(def depths-styles rainbow-styles)
(def depths-count (count depths-styles))

(def all-delimiters "[\\(\\){}\\[\\]]")
(def opening? #{\( \{ \[})

(def ignore? #{:net.cgrand.parsley/unfinished
               :net.cgrand.parsley/unexpected
               :string :comment :char :regex})

(defn collection? [loc]
  (#{:list :vector :map :set :fn} (lang/location-tag loc)))

(defn- depth [loc]
  (loop [loc loc, d 0]
    (if loc
      (recur (zip/up loc) (if (collection? loc) (inc d) d))
      d)))

(defn- delims [loc]
  (let [offset (lang/offset loc)
        down   (zip/down loc)
        len    (lang/location-length loc)
        slen   (-> down zip/leftmost lang/location-length)
        elen   (-> down zip/rightmost lang/location-length)
        d      (-> (depth loc) dec (mod depths-count) inc)]
    [[offset slen d]
     [(+ offset len (- elen)) elen d]]))

(defn- delimiters-tokens [root]
  (let [colls  (lang/search root collection?)]
    (mapcat delims colls)))

(defn- color-delimiters! [editor]
  (let [doc   (ui/attr editor :doc)
        txt   (model/text editor)
        root  (lang/code-zip (lang/parse-tree @doc))
        tokens (delimiters-tokens root)]
    (ui/action
      (when (= txt (model/text editor))
        (ui/apply-style editor tokens depths-styles))))
  editor)

(defn- text-editor-change! [e]
  (let [editor (:source e)]
    (color-delimiters! editor)))

(defn- text-editor-hook [f doc]
  (let [editor (f doc)
        ch     (timeout-channel 500 #'text-editor-change!)]
    (-> editor
      color-delimiters!
      (ui/listen :insert ch)
      (ui/listen :delete ch))))

(def ^:private hooks
  {#'lab.ui.templates/text-editor #'text-editor-hook})

(plugin/defplugin lab.plugin.rainbow-delimiters
  :type  :local
  :hooks hooks)
