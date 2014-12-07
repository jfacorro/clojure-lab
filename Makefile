install:
	lein build
	mkdir -p /usr/lib/clojure-lab/
	sudo cp target/lab.jar /usr/lib/clojure-lab/.
