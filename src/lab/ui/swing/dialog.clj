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
  (let [x       (impl c)
        result  (case (ui/get-attr c :type)
                  :open   (.showOpenDialog x nil)
                  :save   (.showSaveDialog x nil)
                  :custom (.showSaveDialog x nil (ui/get-attr c :accept-label)))]
     [result (.getSelectedFile x)]))

(defn- file-dialog-result [result chosen]
  (condp = result
    nil                          [:invalid-result nil]
    JFileChooser/CANCEL_OPTION   [:cancel chosen]
    JFileChooser/APPROVE_OPTION  [:accept chosen]
    JFileChooser/ERROR_OPTION    [:error  chosen]))

(defn- apply-attr [c k]
  (ui/set-attr c k (ui/get-attr c k)))

(ui/defattributes
  :file-dialog
  (:type [c _ v])
  (:title [c _ v]
    (.setDialogTitle (impl c) v))
  (:selection-type [c _ v]
    (when (selection-type v)
      (.setFileSelectionMode (impl c) (selection-type v))))
  (:visible ^:modify [c _ v]
    (apply-attr c :selection-type)
    (apply-attr c :title)
    (when v
      (->> c
        file-dialog-open
        (apply file-dialog-result)
        (ui/set-attr c :result))))
  (:result [c _ _])
  
  :option-dialog
  (:visible [c _ v]
    (let [x       (impl c)
          title   (ui/get-attr c :title)
          dialog  (.createDialog x title)]
      (.setVisible dialog true)
      (ui/set-attr c :result (options-result (.getValue x)))))
  (:title [c _ _])
  (:icon [c _ v]
    (.setIcon (impl c) (util/icon v)))
  (:message [c _ v]
    (.setMessage (impl c) v))
  (:options [c _ v]
    (when-let [t (options-type v)]
      (.setOptionType (impl c) t))))
