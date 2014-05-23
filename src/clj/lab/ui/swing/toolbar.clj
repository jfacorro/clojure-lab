(ns lab.ui.swing.toolbar
  (:require [lab.ui.core :as ui]
            [lab.ui.protocols :refer [impl to-map listen ignore]]
            [lab.ui.util :refer [defattributes definitializations]]
            [lab.ui.swing.util :as util])
  (:import  [javax.swing JToolBar]))

(definitializations
  :toolbar JToolBar)

(defattributes
  :toolbar
  (:floatable [c _ v]
    (.setFloatable ^JToolBar (impl c) v)))
