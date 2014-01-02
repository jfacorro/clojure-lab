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

(defn- option-dialog-open [c]
  (let [title   (ui/attr c :title)
        msg     (ui/attr c :message)
        options (ui/attr c :options)]
    (JOptionPane/showConfirmDialog
        nil
        ^Object (ui/attr c :message)
        ^String (ui/attr c :title)
        ^int (options-type options))))

(defn- apply-attr
  "Used to ensure that the value of the attribute
is set before processing other attribute's code."
  [c k]
  (ui/attr c k (ui/attr c k)))

(ui/defattributes
  :dialog
  (:result [c _ _])
  (:title [c _ v]
    (.setDialogTitle ^JFileChooser (impl c) v))
  
  :file-dialog
  (:type [c _ v])
  (:current-dir [c _ ^String v]
    (when v
      (.setCurrentDirectory ^JFileChooser (impl c) (java.io.File. v))))
  (:selection-type [c _ v]
    (when (selection-type v)
      (.setFileSelectionMode ^JFileChooser (impl c) (selection-type v))))
  (:visible ^:modify [c _ v]
    (when v
      (->> (reduce apply-attr c [:selection-type :title :current-dir])
        file-dialog-open
        (apply file-dialog-result)
        (ui/attr c :result))))
  
  :option-dialog
  (:visible ^:modify [c _ v]
    (apply-attr c :title)
    (apply-attr c :message)
    (apply-attr c :options)
    (when v
      (->> c
        option-dialog-open
        options-result
        (ui/attr c :result))))
  (:icon [c _ v]
    (.setIcon ^JOptionPane (impl c) (util/icon v)))
  (:message [c _ v]
    (.setMessage ^JOptionPane (impl c) v))
  (:options [c _ v]
    (when-let [t (options-type v)]
      (.setOptionType ^JOptionPane (impl c) t))))
