cd \Juan\Dropbox\Facultad\Tesis\IDE\src
java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n -cp libs/clojure-1.4.0.jar;. clojure.main -i com/cleasure/main.clj 
pause