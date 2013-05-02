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

(defn- tree-node-from-file
  "Creates a tree node with the supplied file as its item.
  If the arg is a directory, all its children are added 
  recursively."
  [file]
  (if (.isDirectory file)
    (let [children (->> file .listFiles 
                        (filter #(-> % .getName (.startsWith ".") not))
                        (sort-by #(if (.isDirectory %) 0 1))
                        (map file-proxy))]
      (ui/tree-node :item file 
                    :content (->> children (map tree-node-from-file) vec)))
    (ui/tree-node :item file)))

(defn tree-from-path
  "Generates the tree for the given path. If its a file
  then the parent directory is considered the root of the 
  tree."
  [ui root-path]
  (let [root  (io/file root-path)
        root  (if (.isDirectory root) root (.getParentFile root))]
    (ui/tree :root (tree-node-from-file root))))
