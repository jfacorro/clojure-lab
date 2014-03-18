(ns lab.test.core.dummy-local-plugin
  (:require [lab.core :as lab]
            [lab.test.core.plugin :refer [hooked hook]]
            [lab.core.keymap :as km]
            [lab.core.plugin :refer [defplugin]]))

(defplugin lab.test.core.dummy-local-plugin
  :type    :local
  :keymaps [(km/keymap :dummy-local
                       :local
                       {:fn :fn :keystroke "ctrl o"})]
  :hooks   {#'hooked #'hook}
  :init!   (fn [app] (swap! (lab/current-document @app) assoc :init? true))
  :unload! (fn [app] (swap! (lab/current-document @app) dissoc :init?)))