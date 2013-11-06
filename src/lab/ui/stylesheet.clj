(ns lab.ui.stylesheet
  (:require [lab.ui [core :as ui]
                    [select :as sel]]))

(defn- apply-style
  [ui [selector attrs]]
  (reduce (fn [ui [attr value]]
            (ui/update ui selector ui/set-attr attr value))
          ui
          attrs))

(defn apply-stylesheet
  "Takes an atom with the root of a (initialized abstract UI) component and
  a stylesheet (map where the key is a selector and the values a map of attributes
  and values) and applies it to the matching components."
  [ui stylesheet]
  (swap! ui #(reduce apply-style % stylesheet)))
  
(comment

(do
  (def ui
    (let [app (init {:name "Clojure Lab - UI dummy"})
          ui  (app :ui)]
      ui))
  (def stylesheet {:split {:border :none}
                   :tabs  {:border :none}
                   :text-editor {:background 0x555555
                                 :font       [:name "Monospaced.plain" :size 14]}})
  (css/apply-stylesheet x stylesheet)
  nil)

)
