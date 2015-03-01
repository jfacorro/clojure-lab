all:
	lein build

clean:
	lein clean

install:
	cp target/lab.jar /usr/local/bin/.
	printf "#! /bin/bash\njava -jar /usr/local/bin/lab.jar &" > /usr/local/bin/clojure-lab
	chmod +x /usr/local/bin/clojure-lab
	chmod 666 /usr/local/bin/lab.jar
