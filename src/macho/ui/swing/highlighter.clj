(ns macho.ui.swing.highlighter
  (:import [javax.swing.text StyleContext SimpleAttributeSet StyleConstants]
           [javax.swing JTextPane SwingUtilities]
           [java.awt Color]
           [java.util.regex Matcher])
  (:require [macho.lang.clojure :as lang :reload true]
            [clojure.set :as set]))

(def style-constants {:bold StyleConstants/Bold,
                      :background StyleConstants/Background,
	             :foreground StyleConstants/Foreground})

(defn rgb-to-int [rgb]
  (int (+ (* (:r rgb) 65536) (* (:g rgb) 256) (:b rgb))))

(defn parse-attrs [stl]
  (let [color-attr #{:foreground :background}
        to-color (fn [[k v]] [k (Color. (rgb-to-int v))])
        attrs (mapcat to-color (filter (comp color-attr key) stl))]
    (apply assoc stl attrs)))

(defn make-style [attrs]
  "Creates a new style with the given
   attributes values."
  (let [style (SimpleAttributeSet.)
        att (parse-attrs attrs)]
    (doseq [[k v] att]
      (.addAttribute style (k style-constants) v))
    style))

(defn init-styles [stls]
  (let [f (fn [[k v]] {k (assoc v :style (make-style (v :style)))})]
    (reduce #(merge %1 (f %2)) stls stls)))

(def ^:dynamic *default* (make-style {:foreground {:r 0 :g 131 :b 131}}))
(def ^:dynamic *syntax* (init-styles lang/syntax))
(def ^:dynamic *higlighting* (atom false))

(defn get-limits [^Matcher m]
  "Using the regex matcher provided returns the
  start and end of the next match."
  (when (. m find) 
    [(. m start) (. m end)]))

(defn limits
  ([ptrn s]
    (let [m (re-matcher (re-pattern ptrn) s)]
      (limits m)))
  ([^Matcher m]
    (lazy-seq
      (when-let [lim (get-limits m)]
        (cons lim (limits m))))))

(defn remove-cr [^String s]
  "Removes carriage returns from the string."
  (.replace s "\r" ""))

(defn apply-style
  ([^JTextPane txt strt end stl]
    (SwingUtilities/invokeLater #(.setCharacterAttributes txt strt end stl true)))
  ([^JTextPane txt stl]
    (SwingUtilities/invokeLater #(.setCharacterAttributes txt stl true))))

(defn high-light [^JTextPane txt-pane]
    (let [doc (.getDocument txt-pane)
          len (.getLength doc)
          text (.getText doc 0 len)]
      (apply-style doc 0 len *default*)
      (doseq [[k v] *syntax*]
        (let [stl (v :style)
              ptrn (v :regex)]
          (doseq [[strt end] (limits ptrn text)]
            (apply-style doc strt (- end strt) stl))))
      (apply-style txt-pane *default*)))




