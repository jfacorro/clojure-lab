(ns com.cleasure.ui.high-lighter
	(:import
		[javax.swing.text StyleContext SimpleAttributeSet StyleConstants]
		[java.awt Color])
	(:require
		[com.cleasure.lang.clojure.keywords :as k]))

(def style-constants {
	:bold			StyleConstants/Bold, 
	:foreground		StyleConstants/Foreground, 
	:font-size		StyleConstants/FontSize,
	:font-family	StyleConstants/FontFamily})

(defn defstyle [attrs]
	(let [style (SimpleAttributeSet.)]
		(doseq [[k v] attrs]
			(.addAttribute style (k style-constants) v))
		style))

(def styles {	:keywords	(defstyle {:bold true :foreground Color/blue :font-family "Consolas" :font-size (int 14)})
				:delimiters	(defstyle {:bold false :foreground Color/red :font-family "Consolas" :font-size (int 14)})
				:default	(defstyle {:bold false :foreground Color/black :font-family "Consolas" :font-size (int 14)})})

(def keywords k/keywords)
(def delimiters #{"(" ")" "{" "}" "[" "]"})

(def styles-map {	:keywords	keywords
		:delimiters	delimiters})

(defn all-index-of [text ptrn]
	"Finds all indexes where ptrn is matched in text and
	return a list with the intervals in which de matches are
	located."
	(let [	len (.length ptrn)]
		(loop [	start	0
				idx 	(.indexOf text ptrn start)
				idxs	[]]
			(if (= idx -1) 
				idxs
				(recur
					idx 
					(.indexOf text ptrn (+ idx len))
					(conj idxs idx))))))

(defn remove-cr [str] 
	"Removes carriage returns from the string."
	(.replace str "\r" ""))

(defn high-light [txt-pane]
	(let [	doc (.getStyledDocument txt-pane)
			text (.getText txt-pane)
			stripped (remove-cr text)]
		(.setCharacterAttributes doc 0 (.length text) (:default styles) true)
		(doseq [[s kws] styles-map]
			(doseq [kw kws]
				(doseq [idx (all-index-of stripped kw)]
					(.setCharacterAttributes doc idx (.length kw) (s styles) true))))
		(.setCharacterAttributes txt-pane (:default styles) true)))
