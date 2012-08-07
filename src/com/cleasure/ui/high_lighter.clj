(ns com.cleasure.ui.high-lighter
	(:import
		[javax.swing.text StyleContext SimpleAttributeSet StyleConstants]
		[java.awt Color])
	(:require
		[com.cleasure.lang.clojure.keywords :as k]
		[clojure.set :as set]))

(def style-constants {
	:bold		StyleConstants/Bold, 
	:foreground	StyleConstants/Foreground, 
	:font-size	StyleConstants/FontSize,
	:font-family	StyleConstants/FontFamily})

(defn defstyle [attrs]
  "Creates a new style with the given
   attributes values."
  (let [style (SimpleAttributeSet.)]
    (doseq [[k v] attrs]
      (.addAttribute style (k style-constants) v))
  style))

(def styles {:keywords (defstyle {:bold true :foreground Color/blue})
             :symbols (defstyle {:bold true :foreground Color/orange})
             :delimiters (defstyle {:bold false :foreground Color/gray})
             :default (defstyle {:bold false :foreground Color/black})})

(def keywords k/keywords)
(def symbols (set (map str (keys (ns-refers *ns*)))))
(def delimiters #{"(" ")" "{" "}" "[" "]"})
(def blanks #{ \( \) \{ \} \[ \] \,  \space \newline \tab })

(def styles-map {:keywords keywords, :delimiters delimiters, :symbols symbols})

(defn blank? [c] (blanks c))

(defn valid-match? [s ptrn idx]
  "Check if ptrn is surrounded by 'blank' characters
  in the given idx for the string s."
  (let [len (count ptrn)
        begin (if (pos? idx) (dec idx) idx)
        end (+ idx len)]
    (or (delimiters ptrn)
        (and
          (<= 0 begin)
          (or (zero? begin) (blank? (.charAt s begin)))
          (pos? end)
          (<= end (count s))
          (or (= end (count s)) (blank? (.charAt s end)))))))

(defn all-indexes [s ptrn]
  "Finds all indexes where ptrn is matched in text and
   returns a list with the indexes in which the matches 
   are located."
  (let [f #(.indexOf s ptrn (inc %1))
        idxs (drop 1 (iterate f -1))]
    (filter
      #(valid-match? s ptrn %1)
      (for [idx idxs :while (<= 0 idx)] idx))))

(defn remove-cr [str] 
  "Removes carriage returns from the string."
  (.replace str "\r" ""))

(defn high-light [txt-pane]
  (let [doc (.getStyledDocument txt-pane)
        text (remove-cr (.getText txt-pane))]
    (.setCharacterAttributes doc 0 (.length text) (:default styles) true)
    (doseq [[stl kws] styles-map]
      (doseq [kw kws]
        (doseq [idx (all-indexes text kw)]
          (.setCharacterAttributes doc idx (.length kw) (stl styles) true))))
    (.setCharacterAttributes txt-pane (:default styles) true)))
