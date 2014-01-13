(ns lab.plugin.markdown-lang
  "Markdown language specification."
  (:require [lab.core :as lab]
            [lab.core [plugin :as plugin]
                      [lang :as lang]]
            [lab.model.document :as doc]))

(def grammar [:expr- #{:title :html-tag :strong :em :bullet :code :link}
              :title #{#"#{1,6}.+\n"
                       #".+\n[-=]+\n"}
              :strong #{#"\*\*(?<! ).+?(?! )\*\*"
                        #"__(?<! ).+?(?! )__"}
              :em #{#"(?<!\*)\*(?!\*)(?<! ).+?(?! )(?<!\*)\*(?!\*)"
                    #"(?<!_)_(?!\_)(?<! ).+?(?! )(?<!_)_(?!\_)"}
              :bullet #"[ \t]{0,3}([-\+\*]|\d+\.) "
              :link #{#"\[.+\]\(.+\)"
                      #"\[.+\]\[.*\]"
                      #" {0,3}\[.+\]:[ \t]+.+"}
              :html-tag #"(?s)</?[^ ][\w]+.*?/?>"
              :code #{#"^(?: {4,}|\t).*"
                      #"(?<!`)`(?!`).+?(?<!`)`(?!`)"
                      #"``.+?``"}
              :whitespace #"[\r\n]+"])

(def styles
 {:title {:color 0xC800C8 :bold true}
  :strong {:color 0x00FF00}
  :em {:color 0x00FFFF}
  :bullet {:color 0xFF6666 :bold true}
  :link {:color 0xFFE64D}
  :html-tag {:color 0x64DCB3}
  :code {:color 0x9566E5}
  :default {:color 0xFFFFFF}})

(def markdown
  {:name     "Markdown"
   :options  {:main      :expr*
              :root-tag  ::root
              :space :whitespace*
              :make-node lang/make-node}
   :grammar  grammar
   :rank     (partial lang/file-extension? "md")
   :styles   styles})

(defn init! [app]
  (swap! app assoc-in [:langs :markdown] markdown))

(plugin/defplugin lab.plugin.markdown-lang
  :init! init!)