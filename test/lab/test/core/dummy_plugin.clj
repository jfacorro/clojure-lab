(ns lab.test.core.dummy-plugin
  (:require [lab.test.core.plugin :refer [hooked hook]]
            [lab.core.keymap :as km]
            [lab.core.plugin :refer [defplugin]]))

(defplugin lab.test.core.plugin
  :keymaps [(km/keymap :dummy-global
                       :global
                       {:fn :fn :keystroke "ctrl o"})
            (km/keymap :dummy-lang
                       :lang :plain-text
                       {:fn :fn :keystroke "ctrl o"})
            (km/keymap :dummy-local
                       :local
                       {:fn :fn :keystroke "ctrl o"})]
  :hooks   {#'hooked #'hook}
  :init!   (fn [app] (swap! app assoc :init? true))
  :unload! (fn [app] (swap! app dissoc :init?)))