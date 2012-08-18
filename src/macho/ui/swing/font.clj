(ns macho.ui.swing.font
  (:import [java.awt Font]))

(def styles {:plain Font/PLAIN
             :bold Font/BOLD
             :italic Font/ITALIC})

(defn font [{s :name ms :styles n :size}]
  (let [style (reduce bit-and (map styles ms))]
    (Font. s style n)))