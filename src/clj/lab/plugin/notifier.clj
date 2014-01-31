(ns lab.plugin.notifier
  (:require [lab.core.plugin :as plugin]
            [lab.ui.core :as ui]
            [lab.ui.templates :as tplts]
            [clojure.repl :as repl]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; UI Error Handler

;; TODO: add link to files when displaying the stack trace.

(defn truncate [n s]
  (if (< n (.length s))
    (str (.substring s 0 n) "...")
    s))

(defn- show-error-info
  [app ex]
  (let [ui     (@app :ui)
        sw     (java.io.StringWriter.)
        _      (.printStackTrace ex (java.io.PrintWriter. sw))
        title  (->> (or (.getMessage ex) ex)
                 str
                 (truncate 50)
                 (str "Error - "))
        tab    (-> (tplts/tab)
                 (ui/update :label ui/attr :text title)
                 (ui/add [:scroll {:border :none}
                           [:text-area {:text (str sw) :read-only true :post-init #(ui/caret-position (:source %2) 0)}]]))]
    (ui/update! ui (ui/parent "bottom") ui/attr :divider-location-right 200)
    (ui/update! ui :#bottom ui/add tab)))

(defn- default-error-handler
  [app]
  (let [handler (proxy [Thread$UncaughtExceptionHandler] []
                  (uncaughtException [thread ex]
                    (try (#'show-error-info app ex)
                    (catch Exception new-ex
                      (println "Exception handler threw an Exception :S " new-ex)
                      (repl/pst ex))))
                  (handle [ex]
                    (#'show-error-info app ex)))
       class-name (-> handler class .getName)]
    ;(System/setProperty "sun.awt.exception.handler" class-name)
    (Thread/setDefaultUncaughtExceptionHandler handler)))

(defn- init! [app]
  (default-error-handler app))

(plugin/defplugin lab.plugin.notifier
  :init! #'init!)