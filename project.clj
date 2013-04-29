(defproject clojure-lab "0.0.1-SNAPSHOT"
  :description "macho (minimal advanced clojure hacking optimizer)"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [net.cgrand/parsley "0.9.1"]
                 [popen "0.2.0"]
                 [leiningen "2.0.0"]
                 [swingrepl "1.3.0" :exclusions [org.clojure/clojure org.clojure/clojure-contrib]]]
  :main macho
  :manifest {"SplashScreen-Image" "logo.png"})
