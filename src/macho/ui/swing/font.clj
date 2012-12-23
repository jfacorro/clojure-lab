(ns macho.ui.swing.font
  (:import [java.awt Font]))

(def styles {:plain Font/PLAIN
             :bold Font/BOLD
             :italic Font/ITALIC})

(defn font [& xs]
  (let [{s :name ms :styles n :size} xs
        style (reduce bit-and (map styles ms))]
    (Font. s style n)))