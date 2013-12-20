(ns lab.ui.swing.dialog
  (:import  [javax.swing JDialog JFileChooser JOptionPane])
  (:use     [lab.ui.protocols :only [abstract impl]])
  (:require [lab.ui.core :as ui]
            [lab.ui.swing.util :as util]))

(ui/definitializations
  :file-dialog   JFileChooser
  :option-dialog JOptionPane
  :dialog        JDialog)

(def ^:private options-result
  {JOptionPane/OK_OPTION      :ok
   JOptionPane/NO_OPTION      :no
   JOptionPane/CANCEL_OPTION  :cancel
   JOptionPane/CLOSED_OPTION  :closed})

(def ^:private options-type
  {:default       JOptionPane/DEFAULT_OPTION
   :yes-no        JOptionPane/YES_NO_OPTION
   :yes-no-cancel JOptionPane/YES_NO_CANCEL_OPTION
   :ok-cancel     JOptionPane/OK_CANCEL_OPTION})

(def ^:private selection-type
  {:dir-only      JFileChooser/DIRECTORIES_ONLY
   :dir-and-file  JFileChooser/FILES_AND_DIRECTORIES
   :file-only     JFileChooser/FILES_ONLY})

(defn- file-dialog-open [c]
  (let [x       ^JFileChooser (impl c)
        result  (case (ui/attr c :type)
                  :open   (.showOpenDialog x nil)
                  :save   (.showSaveDialog x nil)
                  :custom (.showDialog x nil (ui/attr c :accept-label)))]
     [result (.getSelectedFile x)]))

(defn- file-dialog-result [result chosen]
  (condp = result
    nil                          [:invalid-result nil]
    JFileChooser/CANCEL_OPTION   [:cancel chosen]
    JFileChooser/APPROVE_OPTION  [:accept chosen]
    JFileChooser/ERROR_OPTION    [:error  chosen]))

(defn- apply-attr
  "Used to ensure that the value of the attribute
is set before processing other attribute's code."
  [c k]
  (ui/attr c k (ui/attr c k)))

(ui/defattributes
  :file-dialog
  (:type [c _ v])
  (:current-dir [c _ ^String v]
    (when v
      (.setCurrentDirectory ^JFileChooser (impl c) (java.io.File. v))))
  (:title [c _ v]
    (.setDialogTitle ^JFileChooser (impl c) v))
  (:selection-type [c _ v]
    (when (selection-type v)
      (.setFileSelectionMode ^JFileChooser (impl c) (selection-type v))))
  (:visible ^:modify [c _ v]
    (apply-attr c :selection-type)
    (apply-attr c :title)
    (apply-attr c :current-dir)
    (when v
      (->> c
        file-dialog-open
        (apply file-dialog-result)
        (ui/attr c :result))))
  (:result [c _ _])
  
  :option-dialog
  (:visible [c _ v]
    (let [x       ^JOptionPane (impl c)
          title   (ui/attr c :title)
          dialog  (.createDialog x title)]
      (.setVisible dialog true)
      (ui/attr c :result (options-result (.getValue x)))))
  (:title [c _ _])
  (:icon [c _ v]
    (.setIcon ^JOptionPane (impl c) (util/icon v)))
  (:message [c _ v]
    (.setMessage ^JOptionPane (impl c) v))
  (:options [c _ v]
    (when-let [t (options-type v)]
      (.setOptionType ^JOptionPane (impl c) t))))
