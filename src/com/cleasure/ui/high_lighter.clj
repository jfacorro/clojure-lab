(ns com.cleasure.ui.high-lighter
	(:import
		[javax.swing.text StyleContext SimpleAttributeSet StyleConstants]
		[java.awt Color]))

(def style-constants {
	:bold			StyleConstants/Bold, 
	:foreground		StyleConstants/Foreground, 
	:font-size		StyleConstants/FontSize})

(defn make-style [attrs]
	(let [style (SimpleAttributeSet.)]
		(doseq [[k v] attrs]
			(. style addAttribute (k style-constants) v))
		style))

(def styles {	:keywords	(make-style {:bold true :foreground Color/blue})
				:delimiters	(make-style {:bold true :foreground Color/red})
				:default	(make-style {:bold false :foreground Color/black})})

(def keywords #{"def" "defn" "fn" "ns"})
(def delimiters #{"(" ")" "{" "}" "[" "]"})

(def styles-map {	:keywords	keywords
					:delimiters	delimiters})

(defn all-index-of [text ptrn]
	"Finds all indexes where ptrn is matched in text and
	return a list with the intervals in which de matches are
	located."
	(let [len (.length ptrn)]
		(loop [	start	0
				idx 	(. text indexOf ptrn start)
				idxs	[]]
			(if (= idx -1) 
				idxs
				(recur
					idx 
					(. text indexOf ptrn (+ idx len))
					(conj idxs [idx (+ idx len)]))))))

(defn high-light [txt-pane]
	(let [	doc (. txt-pane getStyledDocument)
			text (. txt-pane getText)]
		(. doc setCharacterAttributes 0 (. text length) (:default styles) false)
		(doseq [[s kws] styles-map]
			(doseq [kw kws]
				(println " for " kw " found " (all-index-of text kw))
				(doseq [[strt end] (all-index-of text kw)]
					(. doc setCharacterAttributes strt (dec end) (s styles) false))))))
