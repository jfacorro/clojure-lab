(defproject clojure-lab "0.0.1-SNAPSHOT"
  :description "Clojure Lab - A development environment for Clojure in Clojure."
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [net.cgrand/parsley "0.9.2"]
                 [popen "0.3.0"]
                 [leiningen-core "2.3.4"]
                 ; Logging
                 [org.clojure/tools.logging "0.2.6"]
                 [org.slf4j/slf4j-log4j12 "1.7.5"]
                 ; Repl console
                 [swingrepl "1.3.0" :exclusions [org.clojure/clojure org.clojure/clojure-contrib]]
                 ; Text Editor
                 [com.fifesoft/rsyntaxtextarea "2.5.0"]
                 ; gtk+
                 #_[local/gtk "4.1"]
                 #_[local/gtk-bindings "4.1"]]
  :manifest {"SplashScreen-Image" "logo.png"}
  :aliases  {"build" ["do" "clean," "uberjar"]
             "build-aot" ["with-profile" "aot" "build"]}
  :repositories [["local" "file:repo"]]
  :profiles {:aot {:aot :all}
             :uberjar {:main proto.main
                       :aot  [proto.main]}})
