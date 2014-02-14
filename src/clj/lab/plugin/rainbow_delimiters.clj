(ns lab.plugin.rainbow-delimiters
  (:require [clojure.core.async :as async]
            [lab.ui.core :as ui]
            [lab.util :refer [timeout-channel find-limits]]
            [lab.model.document :as doc]
            [lab.core [plugin :as plugin]
                      [lang :as lang]]))

(def dark-styles
  {0 {:color 0xFFFF00}
   1 {:color 0x555555}
   2 {:color 0x93A8C6}
   3 {:color 0xB0B1A3}
   4 {:color 0x97B098}
   5 {:color 0xAEBED8}
   6 {:color 0xB0B0B3}
   7 {:color 0x90A890}
   8 {:color 0xA2B6DA}
   9 {:color 0x9CB6AD}})

(def light-styles
  {0 {:color 0x88090B}
   1 {:color 0x707183}
   2 {:color 0x7388d6}
   3 {:color 0x909183}
   4 {:color 0x709870}
   5 {:color 0x907373}
   6 {:color 0x6276ba}
   7 {:color 0x858580}
   8 {:color 0x80a880}
   9 {:color 0x887070}})

(def depths-styles light-styles)

(def all-delimiters "[\\({\\[]")

(def ignore? #{:net.cgrand.parsley/unfinished 
               :net.cgrand.parsley/unexpected
               :string :comment :char})

(defn- delimiters-tokens [root-loc delimiters]
  (loop [depth  0
         [[start _] & delims] delimiters
         tokens []]
    (if-not start
      tokens
      (let [[loc pos] (lang/location root-loc start)
            end       (+ -1 pos (lang/node-length (-> loc clojure.zip/up clojure.zip/node)))
            tag       (lang/location-tag loc)
            depth     (if (ignore? tag) depth (inc depth))
            mod-depth (inc (mod depth 9))]
        (recur depth delims
               (if (ignore? tag)
                 tokens
                 (conj tokens [start 1 mod-depth] [end 1 mod-depth])))))))

(defn- color-delimiters! [editor]
  (let [doc   (ui/attr editor :doc)
        txt   (doc/text @doc)
        delimiters (find-limits all-delimiters txt)
        root  (lang/code-zip (lang/parse-tree @doc))
        tokens (delimiters-tokens root delimiters)]
    (ui/action
      (ui/apply-style editor tokens depths-styles)))
  editor)

(defn- text-editor-change! [app e]
  (let [editor (:source e)]
    (color-delimiters! editor)))

(defn- text-editor-hook [f doc]
  (let [editor (f doc)
        hl-ch  (timeout-channel 100 #'text-editor-change!)]
    (-> editor
      color-delimiters!
      (ui/listen :insert hl-ch)
      (ui/listen :delete hl-ch))))

(def ^:private hooks
  {#'lab.ui.templates/text-editor #'text-editor-hook})

(plugin/defplugin lab.plugin.rainbow-delimiters
  :hooks hooks)

