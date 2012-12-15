(ns macho.ui.views 
  (:import [javax.swing JFrame JPanel SwingUtilities]
           [java.awt Color RenderingHints])
  (:require [macho.ui.protocols :as pr]
            [macho.str-code :as sc]
            [macho.ui.swing.util :as ui]))
;;----------------------------------
(defrecord Figure [x y w h ns])
;;----------------------------------
(defn center [f]
  (vector (+ (.x f) (/ (.w f) 2)) (+ (.y f) (/ (.h f) 2))))
;;----------------------------------
(defn draw-line [g f1 f2]
  (apply #(.drawLine g %1 %2 %3 %4) (concat (center f1) (center f2))))
;;----------------------------------
(defn paint-figures [g figs]
  (doseq [f (vals @figs)]
    (let [x   (.x f)
          y   (.y f)
          w   (.w f)
          h   (.h f)
          nsp (.ns f)
          s   (str (.name nsp))
          uses (.uses nsp)
          requires (.requires nsp)]
    (.setColor g Color/BLACK)
    (.drawOval g x y w h)
    (apply #(.drawString g s %1 %2) (center f))

    (.setColor g Color/BLUE)
    (doseq [u uses] (when (@figs u) (draw-line g f (@figs u))))

    (.setColor g Color/CYAN)
    (doseq [r requires] (when (@figs r) (draw-line g f (@figs r)))))))
;;----------------------------------
(defn build-panel [figs]
  (let [panel (proxy [JPanel] []
                (paintComponent [g]
                  (.setRenderingHint g RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON)
                  (proxy-super paintComponent g)
                  (paint-figures g figs)))]
    (doto panel
      (.setBackground Color/LIGHT_GRAY))))
;;----------------------------------
(defn make-figure [ns n fpos]
  (let [[x y] (fpos n)]
    (Figure. x y 20 20 ns)))
;;----------------------------------
(defn circle-position [x y r c]
  (let [pi (* 2 Math/PI)
        dt (/ pi c)]
    (fn [n]
      [(->> n (* dt) Math/cos (* r) (+ x) int)
       (->> n (* dt) Math/sin (* r) (+ y) int)])))
;;----------------------------------
(defn create-figures [nss mx my mw mh]
  (let [x0     (/ mw 2)
        y0     (/ mh 2)
        r      (min x0 y0)
        fpos   (circle-position x0 y0 r (count nss))
        fsize  (circle-position x0 y0 r (count nss))]
    (->> (vals nss) 
         (mapcat #(vector (.name %2) 
                          (make-figure %2 %1 fpos)) (range (count nss)))
         (apply assoc {}))))
;;----------------------------------
(defn paint-ns [project-ns]
  (let [w        500
        h        500
        frame    (JFrame.)
        figures  (atom (create-figures project-ns w h 500 500))
        panel    (build-panel figures)]
    ;(clojure.pprint/pprint @figures)
    (ui/queue-action
      #(doto frame 
          (.setSize w h)
          (.add panel)
          (.setVisible true)))))
;;----------------------------------
(def p "C:\\Juan\\Dropbox\\Facultad\\2012.Trabajo.Profesional\\ide\\src")
(paint-ns (sc/load-project-ns p))
