(ns macho.ui.swing.highlighter
  (:import [javax.swing.text StyleContext SimpleAttributeSet StyleConstants]
           [java.awt Color]
           [java.util.regex Matcher])
  (:require [macho.lang.clojure :as lang :reload true]
            [clojure.set :as set]))

(def style-constants {:bold StyleConstants/Bold, 
	             :foreground StyleConstants/Foreground})

(defn rgb-to-int [rgb]
  (int (+ (* (:r rgb) 65536) (* (:g rgb) 256) (:b rgb))))

(defn parse-attrs [stl]
  (if (:foreground stl)
    (let [rgb (:foreground stl)
          color (Color. (rgb-to-int rgb))]
      (merge stl {:foreground color}))
    stl))

(defn make-style [attrs]
  "Creates a new style with the given
   attributes values."
  (let [style (SimpleAttributeSet.)
        att (parse-attrs attrs)]
    (doseq [[k v] att]
      (.addAttribute style (k style-constants) v))
    style))

(def ^:dynamic *default* (make-style {:foreground {:r 0 :g 0 :b 0}}))
(def ^:dynamic *syntax* lang/syntax)

(:keyword *syntax*)

(defn get-limits [^Matcher m]
  "Using the regex matcher provided returns the
  start and end of the next match."
  (if (. m find) 
    [(. m start) (. m end) (. m group)]
    nil))

(defn limits 
  ([ptrn s]
    (let [m (re-matcher (re-pattern ptrn) s)]
      (limits m)))
  ([m]
    (lazy-seq
      (when-let [lim (get-limits m)]
        (cons lim (limits m))))))

(defn remove-cr [str] 
  "Removes carriage returns from the string."
  (.replace str "\r" ""))

(defn apply-style
  ([txt strt end stl]
    (.setCharacterAttributes txt strt end stl true))
  ([txt stl]
    (.setCharacterAttributes txt stl true)))

(defn high-light [txt-pane]
  (let [doc (.getStyledDocument txt-pane)
        text (remove-cr (.getText txt-pane))
        len (.length text)]
    (apply-style doc 0 len *default*)
    ;(println "highlighting... mofooooo")
    (doseq [[_ v] *syntax*]
      (let [stl (make-style (:style v))
            ptrn (:regex v)] 
        (doseq [[strt end _] (limits ptrn text)]
          (apply-style doc strt (- end strt) stl) nil)))
        (apply-style txt-pane *default*)))
