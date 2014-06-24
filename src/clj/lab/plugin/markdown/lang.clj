(ns lab.plugin.markdown.lang
  "Markdown language specification."
  (:require [clojure.zip :as zip]
            [lab.core :as lab]
            [lab.core [plugin :as plugin]
                      [lang :as lang]
                      [keymap :as km]]
            [lab.model.protocols :as model]
            [lab.ui.core :as ui]
            [lab.model.document :as doc]))

(defn loc->def [loc]
  {:offset (lang/offset loc)
   :name (-> loc zip/down zip/node)})

(defn defs [root]
  (let [loc (lang/code-zip root)]
    (loop [loc (zip/down loc), defs []]
      (if (nil? loc)
        defs
        (recur (zip/right loc)
               (if (= :title (-> loc zip/node :tag))
                 (conj defs (loc->def loc))
                 defs))))))

(def grammar [:expr- #{:title :list :blockquote 
                       :paragraph :element}
              :element-   #{:strong :em :link :html :code :text}
              :text      #"([^\s<>#\-+=\*_\[`\d]|\d+(?!\d*\.))\S*"
              :paragraph [#"(?<=\n)[^-#]" :element* "\n"]
              :title #{#"#{1,6}.+\n"
                       #"[-=][-=^ ]*\n"}
              :strong #{#"\*\*(?<! ).+?(?! )\*\*"
                        #"__(?<! ).+?(?! )__"}
              :em #{#"(?<!\*)\*(?!\*)(?<! ).+?(?! )(?<!\*)\*(?!\*)"
                    #"(?<!_)_(?!\_)(?<! ).+?(?! )(?<!_)_(?!\_)"}
              :list #"[ \t]{0,3}(?:[-\+\*]|\d+\.)[ \t]"
              :link #{#"\[.+?\](\(.+?\)|\[.+?\])"
                      #" {0,3}\[.+?\]:[ \t]+.+"}
              :html #"(?s)</?[^ ][\w]*.*?/?>"
              :code #{#"(?: {4} *|\t+[^-])(?! |[-\+\*]|\d+\.)(?<![-\+\*]|\d+\.).*"
                      #"(?<!`)`(?!`).+?(?<!`)`(?!`)"
                      #"``.+?``"}
              :blockquote #">.*"
              :whitespace #"[\r\n]+"])

(def ^:private styles
 {:title {:color 0xC800C8}
  :strong {:color 0x00FF00}
  :em {:color 0x00FFFF}
  :list {:color 0xFF6666}
  :link {:color 0xFFE64D}
  :html {:color 0x64DCB3}
  :code {:color 0x9566E5}
  :blockquote {:color 0xAAAAAA}
  :default {:color 0xFFFFFF}
  :net.cgrand.parsley/unfinished  {:color 0xFF1111 :italic true}
  :net.cgrand.parsley/unexpected  {:color 0xFF1111 :italic true}})

(defn- resolve-style
  "Used by the syntax highlighting plugin. Takes a tag keyword 
and returns the style defined for that tag. If no style exists 
return the default style."
  [tag]
  (styles tag (:default styles)))

(defn- wrap-in
  "Wraps the current selection in the delim."
  [delim {:keys [app source] :as e}]
  (let [ui (:ui @app)
        [s e] (ui/selection source)
        txt   (model/substring source s e)]
    (ui/action
      (doc/bundle-operations
        (when (not= s e)
          (model/delete source s e))
        (model/insert source s (str delim txt delim)))
      (ui/caret-position source (- (ui/caret-position source) (count delim))))))

(def ^:private keymap
   (km/keymap "Markdown"
     :lang :markdown
     {:fn (partial #'wrap-in "`") :keystroke "ctrl k" :name "Code Snippet"}
     {:fn (partial #'wrap-in "**") :keystroke "ctrl b" :name "Bold"}
     {:fn (partial #'wrap-in "*") :keystroke "ctrl i" :name "Italic"}))

(def markdown
  {:id       :markdown
   :name     "Markdown"
   :options  {:main      :expr*
              :root-tag  ::root
              :space :whitespace*
              :make-node lang/make-node}
   :grammar  grammar
   :definitions defs
   :rank     (partial lang/file-extension? "md")
   :styles   #'resolve-style
   :keymap   keymap})

(defn init! [app]
  (swap! app assoc-in [:langs (:id markdown)] markdown))

(plugin/defplugin lab.plugin.markdown.lang
  :type  :global
  :init! init!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Code for testing out the grammar

(comment

(let [txt "		- This caused an ambiguous match."]
  (-> (doc/document markdown)
    (doc/insert 0 txt)
    lang/parse-tree
    clojure.pprint/pprint))

)


