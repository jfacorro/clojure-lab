(ns lab.plugin.markdown.preview
  (:require [lab.core.plugin :refer [defplugin]]
            [markdown.core :as md]))

(defplugin "Markdown HTML Preview"
  :type  :global)
