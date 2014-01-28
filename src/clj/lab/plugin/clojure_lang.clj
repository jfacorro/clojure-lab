(ns lab.plugin.clojure-lang
  "Clojure language specification."
  (:require [clojure.zip :as zip]
            [lab.core :as lab]
            [lab.ui.core :as ui]
            [lab.core [plugin :as plugin]
                      [lang :as lang]
                      [keymap :as km]]
            [lab.model.protocols :as model]))

(def special-forms #{"def" "if" "do" "let" "quote" "var" "'" "fn" "loop" "recur" "throw"
                     "try" "catch" "finally" "monitor-enter" "monitor-exit" #_"." "new" "set!"})

(def core-vars 
  "Gets all the names for the vars in the clojure.core namespace."
  (->> (the-ns 'clojure.core) ns-interns keys (map str) set))

(def grammar [:expr- #{:number :symbol :keyword :list :string :vector :set :map :regex
                       :comment :meta :fn  :deref :char
                       :quote :syntax-quote :unquote :unquote-splice
                       :reader-var :reader-discard}
              :symbol #"(?<!0x|0|0x[A-Fa-f\d]{42})[a-zA-Z!$%&*+\-\./<=>?_][a-zA-Z0-9!$%&*+\-\./:<=>?_#]*"
              :keyword #"::?#?[\w-_*+\?/\.!>]+"
              :whitespace #"[ \t\r\n,]+"
              :list [#"(?<!\\)\(" :expr* #"(?<!\\)\)"]
              :vector ["[" :expr* "]"]
              :map ["{" :expr* "}"]
              :set ["#{" :expr* "}"]
              :pair- [:expr :expr]
              :meta [#"#?\^" #{:keyword :map :symbol :string}]
              :quote ["'" :expr]
              :syntax-quote ["`" :expr]
              :unquote [#"~(?!@)" :expr]
              :unquote-splice ["~@" :expr]
              :regex #"#\"([^\"\\]*|(\\.))*\""
              :string #"(?s)(?<!#)\".*?(?<!\\)\""
              :char #"\\(.|newline|space|tab|backspace|formfeed|return|u([0-9a-fA-F]{4}|[0-7]{1,2}|[0-3][0-7]{2}))(?![a-zA-Z0-9!$%&*+\-\./:<=>?_#])"
              :number #"(0x[\dA-Fa-f]+|\d(?!x)\d*\.?\d*[MN]?)"
              :reader-var ["#'" :symbol]
              :reader-discard ["#_" :expr]
              :comment #"(#!|;).*[^\n\r]*"
              :deref ["@" :expr]
              :fn ["#(" :expr* ")"]])

(def styles-mapping
  {:symbol #{[:special-form special-forms]
             [:var core-vars]}})

(defn- resolve-style [tag [content]]
  (reduce (fn [x [style pred]]
            (if (pred content) style x))
    tag
    (styles-mapping tag)))

(defn- node-meta
  "If the tag for the node is a symbol
check if its one of the registered symbols."
  [tag content]
  {:style (resolve-style tag content)
   :group lang/*node-group*})

(defn- make-node [tag content]
  (with-meta {:tag tag :length (lang/calculate-length content) :content content}
             (node-meta tag content)))

(def styles
 {:special-form {:color 0xC800C8}
  :var          {:color 0x00FFFF}
  :symbol       {:color 0x64DCB3}
  :delimiter    {:color 0xFFFFFF}
  :accesor      {:color 0x960000}
  :regex        {:color 0xDF6443}
  :keyword      {:color 0x00FF00}
  :namespace    {:color 0x960000}
  :string       {:color 0xE61D43}
  :number       {:color 0xFFFFFF}
  :comment      {:color 0x999988 :bold true}
  :default      {:color 0xFFFFFF}
  :net.cgrand.parsley/unfinished  {:color 0xFF1111 :italic true}
  :net.cgrand.parsley/unexpected  {:color 0xFF1111 :italic true}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Outline

(defn- def? [loc]
  (and (-> loc zip/node :tag (= :list))
       (-> loc zip/down zip/right zip/node
         (as-> x
           (and (= (:tag x) :symbol)
                (.startsWith (-> x :content first) "def"))))))

(defn- find-symbol-to-right [loc]
  (loop [loc loc]
    (if (= :symbol (:tag (zip/node loc)))
      (zip/down loc)
      (recur (zip/right loc)))))

(defn- loc->def [loc]
  {:offset (lang/offset loc)
   :name (-> loc zip/down zip/right zip/right find-symbol-to-right zip/node)})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Keymap commands

(defn- insert-tab [app e]
  (let [editor (:source e)
        offset (ui/caret-position editor)]
    (model/insert editor offset "  ")))

(def delimiters
  {\( \)
   \[ \]
   \{ \}
   \" \"})

(defn- balance-delimiter [app e]
  (let [editor  (:source e)
        opening (:char e)
        closing (delimiters opening)
        offset  (ui/caret-position editor)]
    (model/insert editor offset (str opening closing))
    (ui/caret-position editor (inc offset))))

;; Comment

(defn- find-start-of-line
  "Finds the offset for the start of the current line,
which is the offset after the first previous \\newline."
  [offset text]
  (loop [offset offset]
    (if (or (zero? offset) (= (get text (dec offset)) \newline))
      offset
      (recur (dec offset)))))

(defn- comment? [offset text]
  (= (get text offset) \;))

(defn- comment-length [offset text]
  (let [n (count text)]
    (loop [offset offset
           len    0]
      (if (or (>= offset n) (= (get text offset) \newline) (not= (get text offset) \;))
        len
        (recur (inc offset) (inc len))))))

(defn- toggle-comment [app e]
  (let [editor (:source e)
        text   (model/text editor)
        offset (-> editor ui/caret-position (find-start-of-line text))
        len    (comment-length offset text)]
    (if (pos? len)
      (model/delete editor offset (+ offset len))
      (model/insert editor offset ";;"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Find definitions

(defn definitions
  "Returns a sequence of definitions using the def? and 
loc->def functions specified in the language."
  [root]
  (let [loc (zip/down (lang/code-zip root))]
    (loop [loc loc, defs []]
      (if-not loc
        defs
        (recur (zip/right loc)
               (if (def? loc)
                 (conj defs (loc->def loc))
                 defs))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Delimiters matching

(def ^:private delimiter? (set "()[]{}"))

(def ^:private closing? (set ")]}"))

(defn- char-at [[offset [loc pos]]]
  (when loc
    (-> (zip/node loc) (get (- offset pos)))))

(defn- find-matching-delimiter [loc closing?]
  (if closing?
    (-> loc zip/leftmost lang/offset)
    (-> loc zip/rightmost lang/offset)))

(defn- delimiter-match
  "Checks that the character in offset is a delimiter
and returns the offset of its matching delimiter."
  [doc offset]
  (let [root        (-> doc lang/parse-tree lang/code-zip)
        prev-offset (when (pos? offset) (dec offset))
        next-offset (when (< offset (model/length doc)) offset)
        [offset [loc pos]] (->> [next-offset prev-offset]
                             (filter identity)
                             (map (juxt identity (partial lang/location root)))
                             (filter (comp delimiter? char-at))
                             first)
        delim       (when offset (char-at [offset [loc pos]]))]
    (when delim
      [offset (find-matching-delimiter loc (closing? delim))])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Keymap

(def ^:private keymap
  (km/keymap 'lab.plugin.clojure-lang :lang
    {:fn ::insert-tab :keystroke "tab" :name "Insert tab"}
    {:fn ::balance-delimiter :keystroke "(" :name "Balance parenthesis"}
    {:fn ::balance-delimiter :keystroke "{" :name "Balance curly brackets"}
    {:fn ::balance-delimiter :keystroke "[" :name "Balance square brackets"}
    {:fn ::balance-delimiter :keystroke "\"" :name "Balance double quotes"}
    {:fn ::toggle-comment :keystroke "alt c" :name "Comment code"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Language definition

(def clojure
  (lang/map->Language
    {:name      "Clojure"
     :options   {:main      :expr*
                 :root-tag  ::root
                 :space :whitespace*
                 :make-node #'make-node}
     :grammar   grammar
     :rank      (partial lang/file-extension? "clj")
     :styles    styles
     :definitions #'definitions
     :delimiter-match #'delimiter-match
     :keymap    keymap}))

(defn init! [app]
  (swap! app assoc-in [:langs :clojure] clojure))

(plugin/defplugin lab.plugin.clojure-lang
  :init! init!)
