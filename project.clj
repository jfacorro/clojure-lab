(defproject clojure-lab "0.1.0-beta"
  :description "Clojure Lab - A development environment for Clojure in Clojure."
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.match "0.2.1"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]

                 [net.cgrand/parsley "0.9.2"]
                 [popen "0.3.0"]
                 [leiningen "2.3.4"]
                 [org.clojure/tools.nrepl "0.2.3"]

                 [com.cemerick/pomegranate "0.3.0"]

                 [org.clojure/tools.logging "0.2.6"]
                 [org.slf4j/slf4j-log4j12 "1.7.7"]

                 [markdown-clj "0.9.44"]

                 [clojure-watch "0.1.9"]]
  :plugins       [[lein-cloverage "1.0.2"]]
  :uberjar-name  "lab.jar"
  :java-source-paths ["src/java"]
  :source-paths ["src/clj"]
  :manifest {"SplashScreen-Image" "logo.png"}
  :aliases  {"dev" ["do" "clean," "javac," "run-dev"]
             "run-dev" ["with-profile" "dev" "trampoline" "run"]
             "build" ["do" "clean," "uberjar"]
             "build-aot" ["with-profile" "aot" "build"]}
  :repositories [["local" "file:repo"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]]
                   :main lab.main
                   :debug true}
             :aot {:aot :all}
             :uberjar {:main lab.main
                       :aot [lab.main #"lab.ui.*" #"lab.core.*"]}})
