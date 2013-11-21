(ns lab.plugin.file-tree
  (:use [lab.core.plugin :only [defplugin]])
  (:require [lab.core.keymap :as km]
            [lab.plugin.main-ui :as main]
            [lab.ui [core :as ui]
                    [tree :as tree]]))

(defn- open-document-tree
  "Handler for the click event of an item in the tree."
  [app {:keys [source click-count]}]
  (when (= click-count 2)
    (let [^java.io.File file (ui/selected source)]
      (when-not (.isDirectory file)
        (main/open-document app (.getCanonicalPath file))))))

(defn- file-tree [app file]
  [:tab {:title (.getName file) :border :none}
        [:scroll [:tree {:id      "file-tree"
                         :on-click (partial #'open-document-tree app)
                         :root     (tree/load-dir file)}]]])

(defn- open-project
  [app _]
  (let [file-dialog   (ui/init [:file-dialog {:type           :open, 
                                              :selection-type :dir-only, 
                                              :visible        true, 
                                              :title          "Open Directory"}])
        [result dir] (ui/attr file-dialog :result)]
    (when dir
      (ui/update! (:ui @app) :#left-controls ui/add (file-tree app dir)))))

(def ^:private keymaps
  [(km/keymap (ns-name *ns*)
              :global
              {:category "Project" :name "Open..." :fn #'open-project :keystroke "ctrl P"})])

(defplugin file-tree
  :keymaps keymaps)