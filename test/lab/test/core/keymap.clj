(ns lab.test.core.keymap
  (:refer-clojure :exclude [remove find])
  (:require [clojure.test :refer [deftest is run-tests]]
            [lab.core.keymap :as km :refer [append remove keymap find]]))

(def global (keymap :global-keymap :global
                    {:fn :*-global :keystroke "ctrl *"}))

(def lang   (keymap :lang-keymap :lang :plain-text
                    {:fn :ctrl-a-lang :keystroke "ctrl a"}
                    {:fn :ctrl-b-lang :keystroke "ctrl b"}))

(def local  (keymap :local-keymap :local
                    {:fn :ctrl-a-local :keystroke "ctrl a"}
                    {:fn :alt-a-local :keystroke "alt a"}
                    {:fn :alt-a-local :keystroke "alt l"}))

(deftest creation
  (is (= :global (:type global)))
  (is (= :global-keymap (:name global)))
  (is (= 1 (-> global :bindings count)))
    
  (is (= :lang (:type lang)))
  (is (= :lang-keymap (:name lang)))
  (is (= 2 (-> lang :bindings count)))
  (is (= :plain-text (:lang lang )))

  (is (= :local (:type local)))
  (is (= :local-keymap (:name local)))
  (is (= 3 (-> local :bindings count))))

(deftest append-and-find
  (let [global-lang       (append global lang)
        global-lang-local (append global-lang local)
        ctrl-a            (#'km/ks->set "ctrl a")
        alt-a             (#'km/ks->set "alt a")
        ctrl-b            (#'km/ks->set "ctrl b")]

    (is (nil? (:fn (find global ctrl-a))))
    (is (nil? (:fn (find global ctrl-b))))

    (is (= :ctrl-a-lang (:fn (find global-lang ctrl-a))))
    (is (= :ctrl-b-lang (:fn (find global-lang ctrl-b))))

    (is (= :ctrl-a-local (:fn (find global-lang-local ctrl-a))))
    (is (= :alt-a-local (:fn (find global-lang-local alt-a))))
    (is (= :ctrl-b-lang (:fn (find global-lang-local ctrl-b))))))

(deftest remove-and-find
  (let [global-lang-local (-> global (append lang) (append local))
        global-lang       (remove global-lang-local :local-keymap)
        global-local      (remove global-lang-local :lang-keymap)
        lang-local        (remove global-lang-local :global-keymap)
        ctrl-*            (#'km/ks->set "ctrl *")
        ctrl-a            (#'km/ks->set "ctrl a")
        ctrl-b            (#'km/ks->set "ctrl b")
        alt-a             (#'km/ks->set "alt a")]
    (is (nil? (find global-lang alt-a)))
    (is (= :ctrl-a-lang (:fn (find global-lang ctrl-a))))

    (is (nil? (find global-local ctrl-b)))
    (is (= :ctrl-a-local (:fn (find global-local ctrl-a))))

    (is (nil? (find lang-local ctrl-*)))))
