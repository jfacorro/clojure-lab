cd \Juan\Dropbox\Facultad\Tesis\IDE\src
java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n -cp ../lib/clojure-1.4.0.jar;. clojure.main -i macho/core.clj -m macho.core
pause