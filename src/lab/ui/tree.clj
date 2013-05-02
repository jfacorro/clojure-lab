(ns lab.ui.tree
  (:require [clojure.java.io :as io]
            [lab.ui.core :as ui]
            [lab.ui.protocols :as uip]))

(defn file-proxy
  "Creates a proxy that overrides the toString method
  for the File class so that it returns the file/directory's
  name."
  [file]
  (proxy [java.io.File] [(.getPath file)]
    (toString []
      (.getName this))))

(defn- hidden?
  "Returns true for files or directories that begin with a dot."
  [file]
  (-> file .getName (.startsWith ".")))

(defn- tree-node-from-file
  "Creates a tree node with the supplied file as its item.
  If the arg is a directory, all its children are added
  recursively."
  [file]
  (if (.isDirectory file)
    (let [children (->> file .listFiles 
                     (filter (comp not hidden?))
                     (sort-by #(if (.isDirectory %) 0 1))
                     (map file-proxy))]
      (ui/tree-node :item file 
                    :content (->> children (map tree-node-from-file) vec)))
    (ui/tree-node :item file)))

(defn tree-from-path
  "Generates the tree for the given path. If its a file
  then the parent directory is considered the root of the 
  tree."
  [root-path handler]
  (let [root  (io/file root-path)
        root  (if (.isDirectory root) root (.getParentFile root))]
    (ui/tree :on-dbl-click handler
             :root (tree-node-from-file root))))
