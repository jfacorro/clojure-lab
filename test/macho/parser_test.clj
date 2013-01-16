(ns macho.parser-test
  (:require [clojure.pprint :as pp]
            [macho.parser :as p]))

(future
  (let [;path ".\\src\\macho\\parser.clj" 
        path "D:/Work/00.Docs/2012.12.Diciembre/20121221.BBVA.Multiproducto/sql_generator/sql_generator.clj"
        code (slurp path)
        tree (time (p/parse code))]))
