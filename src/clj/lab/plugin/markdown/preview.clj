(ns lab.plugin.markdown.preview
  (:require [lab.core.plugin :refer [defplugin]]
            [lab.ui.core :as ui]
            [markdown.core :as md]))

(defplugin "Markdown HTML Preview"
  :type  :global)

(-> 
  (ui/init
    [:window {:visible true
              :size [400 400]
              :listen [:closing (fn [e] (ui/attr (:source e) :visible false))]}
     [:text-editor {:content-type "text/html"
;;                    :color 0xFFFFFF
;;                    :background 0x333333
                    :read-only true}]])
  (ui/update :text-editor ui/attr :text (md/md-to-html-string "**bla**")))
