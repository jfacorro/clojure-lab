(ns lab.ui.stylesheet
  (:require [lab.ui [core :as ui]
                    [select :as sel]]))

(defn- apply-style
  [ui [selector attrs]]
  (println selector)
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