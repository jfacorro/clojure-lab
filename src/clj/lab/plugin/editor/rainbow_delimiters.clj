(ns lab.plugin.editor.rainbow-delimiters
  (:require [clojure.zip :as zip]
            [lab.ui.core :as ui]
            [lab.util :refer [timeout-channel find-limits]]
            [lab.model.document :as doc]
            [lab.model.protocols :as model]
            [lab.core [plugin :as plugin]
                      [lang :as lang]
                      [keymap :as km]
                      [main :as main]]))

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

(defn- color-delimiters! [editor & [styles]]
  (let [doc   (ui/attr editor :doc)
        txt   (model/text editor)
        root  (lang/code-zip (lang/parse-tree @doc))
        tokens (delimiters-tokens root)
        styles (or styles (-> @doc :lang :styles))]
    (ui/action
      (when (= txt (model/text editor))
        (ui/apply-style editor tokens styles))))
  editor)

(defn- text-editor-change! [e]
  (let [editor (:source e)]
    (color-delimiters! editor depths-styles)))

(defn- text-editor-init [editor]
  (let [ch (timeout-channel 500 #'text-editor-change!)]
    (-> editor
      (color-delimiters! depths-styles)
      (ui/update-attr :stuff assoc ::listener ch)
      (ui/listen :insert ch)
      (ui/listen :delete ch))))

(defn- text-editor-unload [editor]
  (let [ch (::listener (ui/stuff editor))]
    (-> editor
      (ui/update-attr :stuff dissoc ::listener)
      (ui/ignore :insert ch)
      (ui/ignore :delete ch)
      color-delimiters!)))

(defn- toogle-rainbow
  [{:keys [source app] :as e}]
  (let [ui (:ui @app)
        id (ui/attr source :id)]
    (if (-> source ui/stuff ::listener)
      (ui/update! ui (ui/id= id) text-editor-unload)
      (ui/update! ui (ui/id= id) text-editor-init))))

(def ^:private keymap
  (km/keymap "Rainbow Delimiters" :local
    {:fn ::toogle-rainbow :keystroke "meta p" :name "Toogle Rainbow Delimiters"}))

(defn init! [app]
  (let [ui     (:ui @app)
        editor (main/current-text-editor @ui)
        id     (ui/attr editor :id)]
    (ui/update! ui (ui/id= id) #(-> % text-editor-init (ui/listen :key keymap)))))

(defn unload! [app]
  (let [ui (:ui @app)
        id (ui/attr (main/current-text-editor @ui) :id)]
    (ui/update! ui (ui/id= id) #(-> % text-editor-unload (ui/ignore :key keymap)))))

(plugin/defplugin lab.plugin.editor.rainbow-delimiters
  :type    :local
  :init!   init!
  :unload! unload!)
