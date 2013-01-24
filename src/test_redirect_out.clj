(ns out.redirect
  (:import [java.io PrintStream OutputStream OutputStreamWriter]
           [javax.swing JFrame JTextArea JButton JOptionPane]
           [java.awt BorderLayout]
           [java.awt.event MouseAdapter]))

(defn on-click [cmpt f]
  (.addMouseListener cmpt 
    (proxy [MouseAdapter] []
      (mouseClicked [e] (f)))))

(defn prompt [msg]
  (JOptionPane/showMessageDialog nil msg))

(defn print-prompt [msg]
  (println msg)
  (. System/out println msg)
  (prompt msg))

(let [main   (JFrame.)
      txt    (JTextArea.)
      btn    (JButton.)
      stream (proxy [OutputStream] []
                (write
                  ([b off len] 
                    ;(prompt (String. b off len)) 
                    (. txt append (String. b off len)))
                  ([b]
                    ;(prompt (String. b)) 
                    (. txt append (String. b)))))
      out (PrintStream. stream true)]
  (System/setOut out)

  (on-click btn #(print-prompt "Testing output redirection"))
  (doto btn
    (.setText "Print"))
    (doto main
      (.add txt)
      (.add btn BorderLayout/NORTH)
      (.setSize 500 500)
      (.setVisible true)))

(println (in-ns 'clojure.core))

(def ^:dynamic *out-custom* (java.io.OutputStreamWriter. System/out))
(def ^:dynamic *out-original* (java.io.OutputStreamWriter. System/out))
(def ^:dynamic *out* *out-custom*)

