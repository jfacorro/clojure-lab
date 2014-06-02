(ns lab.plugin.clojure.lang
  "Clojure language specification."
  (:require [clojure.zip :as zip]
            [clojure.string :as str]
            [lab.core :as lab]
            [lab.ui.core :as ui]
            [lab.core [plugin :as plugin]
                      [lang :as lang]
                      [keymap :as km]]
            [lab.model.protocols :as model]))

(def special-forms #{"def" "if" "do" "let" "quote" "var" "'" "fn" "loop" "recur" "throw"
                     "try" "catch" "finally" "monitor-enter" "monitor-exit" "." "new" "set!"})

(def core-vars 
  "Gets all the names for the vars in the clojure.core namespace."
  (->> (the-ns 'clojure.core) ns-interns keys (map str) set))

(def grammar [:expr- #{:number :symbol :keyword :list :string :vector :set :map :regex
                       :comment :meta :fn  :deref :char
                       :quote :syntax-quote :unquote :unquote-splice
                       :reader-var :reader-discard}
              :symbol #"(?<!0x|0|0x[A-Fa-f\d]{42})[a-zA-Z¡!$%&*+\-\./<=>¿?_][a-zA-Z0-9¡!$%&*+\-\./:<=>¿?_#]*"
              :keyword #"::?#?[\w-_*+\?/\.!>]+"
              :whitespace #"[ \t\r\n,]+"

              :list ["(" :expr* ")"]
              :vector ["[" :expr* "]"]
              :map ["{" :expr* "}"]
              :set ["#{" :expr* "}"]
              :fn ["#(" :expr* ")"]

              :meta [#"#?\^" #{:keyword :map :symbol :string}]
              :quote ["'" :expr]
              :syntax-quote ["`" :expr]
              :unquote [#"~(?!@)" :expr]
              :unquote-splice ["~@" :expr]
              :regex #"#\".*?(?<!\\)\""                  ;; Doesn't handle \\" as a possible termination.
              :string #"(?s)(?<!#)\".*?(?<!\\)\""        ;; Doesn't handle \\" as a possible termination.

              :char #"\\(.|newline|space|tab|backspace|formfeed|return|u([0-9a-fA-F]{4}|[0-7]{1,2}|[0-3][0-7]{2}))(?![a-zA-Z0-9!$%&*+\-\./:<=>?_#])"
              :number #"(0x[\dA-Fa-f]+|\d(?!x)\d*\.?\d*[MN]?)"
              :reader-var ["#'" :symbol]
              :reader-discard ["#_" :expr]
              :comment #"(#!|;).*[\n\r]"
              :deref ["@" :expr]])

(def styles-mapping
  {:symbol #{[:special-form special-forms]
             [:var core-vars]}})

(defn- resolve-style [tag [content]]
  (if (= tag :symbol)
    (reduce (fn [x [style pred]]
              (if (pred content) style x))
      tag
      (styles-mapping tag))
    tag))

(declare build-scope)

(defn- node-meta
  "If the tag for the node is a symbol
check if its one of the registered symbols."
  [tag content]
  {:style (resolve-style tag content)
   :group lang/*node-group*
   :scope (build-scope {:tag tag :content content})})

(defn- make-node [tag content]
  (with-meta {:tag tag :length (lang/calculate-length content) :content content}
             (node-meta tag content)))

(def styles
 {:whitespace   {:color 0xFFFFFF}
  :symbol       {:color 0x64DCB3}
  :keyword      {:color 0x00FF00}
  :special-form {:color 0xC800C8}
  :var          {:color 0x00FFFF}
  
  :meta         {:color 0xFFFFFF}
  :quote        {:color 0xFFFFFF}
  :syntax-quote {:color 0xFFFFFF}
  :unquote      {:color 0xFFFFFF}
  :unquote-splice {:color 0xFFFFFF}

  :regex        {:color 0xDF6443}
  :string       {:color 0xE61D43}
  :char         {:color 0xE61D43}
  :number       {:color 0xFFFFFF}
  :reader-var   {:color 0xFFFFFF}

  :reader-discard {:color 0x999988}
  :comment      {:color 0x999988}

  :deref        {:color 0xFFFFFF}

  ;; Delimiters and forms using them
;;  :fn           {:color 0xFFFFFF}
;;
;;  :vector       {:color 0xFFFF00}
;;  :list         {:color 0xFFFF00}
;;  :map          {:color 0xFFFF00}
;;  :set          {:color 0xFFFF00}

  :default      {:color 0xFFFFFF}
  :net.cgrand.parsley/unfinished  {:color 0xFF1111 :italic true}
  :net.cgrand.parsley/unexpected  {:color 0xFF1111 :italic true}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Context

(defn node-tag= [tag node]
  (= tag (:tag node)))

(def node-list? (partial node-tag= :list))
(def node-symbol? (partial node-tag= :symbol))
(def node-vector? (partial node-tag= :vector))
(def node-map? (partial node-tag= :map))
(def node-set? (partial node-tag= :set))
(def node-whitespace? (partial node-tag= :whitespace))

(defn node-nth [node n]
  (loop [[x & xs] (rest (:content node))
         n        n]
    (cond
      (node-whitespace? x)
        (recur xs n)
      (and x (pos? n))
        (recur xs (dec n))
      (and x xs) ; Don't return the last item
        x)))

(defn node-first [node]
  (node-nth node 0))

(defn node-second [node]
  (node-nth node 1))

(defn node-count [node]
  (loop [[x & xs] (:content node)
         n        0]
    (cond
      (node-whitespace? x)
        (recur xs n)
      x
        (recur xs (inc n))
      :else
        (- n 2))))

(defn- node-children [node]
  (map (partial node-nth node)
       (range 0 (node-count node))))

(defmulti build-scope
  (fn [x]
    (if (= ::root (:tag x))
      (:tag x)
      (:content (node-first x)))))

(defn- symbols-in-destructure [x]
  (cond
    (or (node-vector? x) (node-map? x))
      (mapcat symbols-in-destructure (node-children x))
    (node-symbol? x)
      [x]))

(defn- symbols-in-binding-vector
  [v]
  (->> (iterate (partial + 2) 0)      ; take nodes in even indexes
       (take (/ (node-count v) 2))
       (map (partial node-nth v))
       (mapcat symbols-in-destructure)))

(defn- name-symbol-in-def
  [node]
  (let [name-sym (first (drop-while #(and (-> % node-symbol? not)   ; look for a symbol
                                          (-> % node-vector? not))  ; and a vector
                                    (drop 1 (node-children node))))]
    (when (node-symbol? name-sym) ; only a symbol is valid
      name-sym)))

(defn- node-def?
  [{[x] :content :as node}]
  (and (string? x)
       (.startsWith ^String x "def")))

(defn- check-for-definition
  [{:keys [tag] :as node}]
  (when (and (= :list tag) (node-def? (node-first node)))
    (name-symbol-in-def node)))

(defn- symbols-in-argument-vector
  [children]
  (let [args (first (drop-while (comp not node-vector?) children))]
    (mapcat symbols-in-destructure (node-children args))))

(defn- build-scope-for-def
  [x]
  (symbols-in-argument-vector (node-children x)))

(defmethod build-scope ::root
  [x]
  (reduce (fn [acc node]
            (if-let [sym-node (check-for-definition node)]
              (conj acc sym-node)
              acc))
          []
          (:content x)))

(defmethod build-scope ["let"]
  [x]
  (symbols-in-binding-vector (node-second x)))

(defmethod build-scope ["binding"]
  [x]
  (symbols-in-binding-vector (node-second x)))

(defmethod build-scope ["loop"]
  [x]
  (symbols-in-binding-vector (node-second x)))

(defmethod build-scope ["defn"]
  [x]
  (build-scope-for-def x))

(defmethod build-scope ["defn-"]
  [x]
  (build-scope-for-def x))

(defmethod build-scope ["fn"]
  [x]
  (build-scope-for-def x))

(defmethod build-scope :default [x] {})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Outline

(defn- def? [loc]
  (and (= :list (-> loc zip/node :tag))
       (-> loc zip/down zip/right zip/node
         (as-> x
           (and (= (:tag x) :symbol)
                (.startsWith (-> x :content first str) "def"))))))

(defn- find-symbol-to-right [init-loc]
  (loop [loc init-loc]
    (cond
      (nil? loc)
        (-> init-loc zip/left zip/down)
      (= :symbol (:tag (zip/node loc)))
        (zip/down loc)
      :else
        (recur (zip/right loc)))))

(defn- loc->def [loc]
  {:offset (lang/offset loc)
   :name (-> loc zip/down zip/right zip/right find-symbol-to-right zip/node)})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Keymap commands

(defn- insert-tab [e]
  (let [editor (:source e)
        offset (ui/caret-position editor)]
    (model/insert editor offset "  ")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Namespace

(defn- ns-list?
  "Takes an parse tree node and returns true if it is
  an ns form or false otherwise."
  [node]
  (and (node-list? node)
       (as-> (node-first node) x
         (when (and (node-symbol? x)
                    (= "ns" (-> x :content first)))
           (name-symbol-in-def node)))))

(defn find-namespace
  "Takes a document and tries to find an ns form in it.
  If there is one, then the namespace name is returned,
  otherwise it returns the default if provided."
  [doc & {:keys [default]}]
  (let [root     (lang/parse-tree doc)
        children (:content root)
        ns-node  (loop [[node & nodes] children]
                   (cond
                     (and node (ns-list? node))
                       (ns-list? node)
                     node
                       (recur nodes)))]
    (or (and ns-node (-> ns-node :content first))
        default)))

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

(def ignore? #{:net.cgrand.parsley/unfinished
               :net.cgrand.parsley/unexpected
               :string :comment :char :regex})

(def ^:private delimiter? (set "()[]{}"))

(def ^:private closing? (set ")]}"))

(defn- char-at [[offset [loc pos]]]
  (when loc
    (-> loc zip/node (get (- offset pos)))))

(defn- find-matching-delimiter [loc closing?]
  (if closing?
    (-> loc zip/leftmost lang/offset)
    (-> loc zip/rightmost lang/offset)))

(defn- delimiter-match
  "Checks that the character in offset is a delimiter
and returns the offset of its matching delimiter."
  [doc offset]
  (let [root        (lang/code-zip (lang/parse-tree doc))
        prev-offset (when (pos? offset) (dec offset))
        next-offset (when (< offset (model/length doc)) offset)
        [offset [loc pos]] (->> [next-offset prev-offset]
                             (filter (comp not nil?))
                             (map (juxt identity (partial lang/location root)))
                             (filter (comp delimiter? char-at))
                             first)
        tag         (lang/location-tag loc)
        delim       (when offset (char-at [offset [loc pos]]))]
    (when (and (not (ignore? tag)) delim)
      [offset (find-matching-delimiter loc (closing? delim))])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Keymap

(def ^:private keymap
  (km/keymap "Clojure"
    :lang :clojure
    {:fn ::insert-tab :keystroke "tab" :name "Insert tab"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Autcompletion

(defn- adjacent-string
  "Returns the first location that's adjacent
to the current in the direction specified."
  [loc dir]
  (lang/select-location (dir loc)
                        dir
                        lang/loc-string?))


(defn- symbols-in-scope
  "Gets all the symbols in the current location's scope."
  [loc]
  (when loc
    (->> (zip/node loc)
         meta
         :scope
         (map (comp first :content)))))

(defn- symbols-in-scope-from-location
  "Starting at the location specified, goes up the parse tree
collecting the symbols in scope from every parent node and the
nodes in the first level."
  [{:keys [source] :as e}]
  (let [root    (-> @(ui/attr source :doc)
                    lang/parse-tree
                    lang/code-zip)
        [loc _] (lang/location root (ui/caret-position source))]
    (loop [loc     loc
           symbols (into #{} (symbols-in-scope loc))]
      (if-not loc
        symbols
        (recur (zip/up loc)
               (into symbols (symbols-in-scope loc)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Language definition

(def clojure
  (lang/map->Language
    {:id        :clojure
     :name      "Clojure"
     :options   {:main      :expr*
                 :root-tag  ::root
                 :space :whitespace*
                 :make-node #'make-node}
     :grammar   grammar
     :rank      (partial lang/file-extension? "clj")
     :styles    styles
     :definitions #'definitions
     :delimiter-match #'delimiter-match
     :keymap    keymap
     :autocomplete [#'symbols-in-scope-from-location]}))

(defn init! [app]
  (swap! app assoc-in [:langs (:id clojure)] clojure))

(plugin/defplugin lab.plugin.clojure.lang
  :type :global
  :init! init!)
