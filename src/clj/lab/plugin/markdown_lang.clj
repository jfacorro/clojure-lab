(ns lab.plugin.markdown-lang
  "Markdown language specification."
  (:require [clojure.zip :as zip]
            [lab.core :as lab]
            [lab.core [plugin :as plugin]
                      [lang :as lang]]
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
              :link #{#"\[.+?\]\(.+?\)"
                      #"\[.+?\]\[.*?\]"
                      #" {0,3}\[.+?\]:[ \t]+.+"}
              :html #"(?s)</?[^ ][\w]+.*?/?>"
              :code #{#"(?: {4} *|\t+)(?! |[-\+\*]|\d+\.)(?<![-\+\*]|\d+\.).*"
                      #"(?<!`)`(?!`).+?(?<!`)`(?!`)"
                      #"``.+?``"}
              :blockquote #">.*"
              :whitespace #"[\r\n]+"])

(def styles
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

(def markdown
  {:name     "Markdown"
   :options  {:main      :expr*
              :root-tag  ::root
              :space :whitespace*
              :make-node lang/make-node}
   :grammar  grammar
   :definitions defs
   :rank     (partial lang/file-extension? "md")
   :styles   styles})

(defn init! [app]
  (swap! app assoc-in [:langs :markdown] markdown))

(plugin/defplugin lab.plugin.markdown-lang
  :type  :global
  :init! init!)
