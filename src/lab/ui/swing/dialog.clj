(ns lab.ui.swing.dialog
  (:import  [javax.swing JDialog JFileChooser JOptionPane])
  (:use     [lab.ui.protocols :only [Visible abstract impl]])
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

(extend-protocol Visible
  JFileChooser
  (visible? [this]
    (.isVisible this))
  (hide [this]
    (.setVisible this false))
  (show [this]
    (let [c           (abstract this)
          dialog-type (ui/get-attr c :type)
          result      (case dialog-type
                        :open   (.showOpenDialog this nil)
                        :save   (.showSaveDialog this nil)
                        :custom (.showSaveDialog this nil (ui/get-attr c :accept-label)))
          chosen      (.getSelectedFile this)]
      (condp = result
        nil                          [:invalid-result nil]
        JFileChooser/CANCEL_OPTION   [:cancel chosen]
        JFileChooser/APPROVE_OPTION  [:accept chosen]
        JFileChooser/ERROR_OPTION    [:error  chosen])))
  
  JOptionPane
  (visible? [this]
    (.isVisible this))
  (hide [this]
    (.setVisible this false))
  (show [this]
    (let [c       (abstract this)
          title   (ui/get-attr c :title)
          dialog  (.createDialog this title)]
    (.setVisible dialog true)
    (options-result (.getValue this)))))

(def ^:private options-type
  {:default       JOptionPane/DEFAULT_OPTION
   :yes-no        JOptionPane/YES_NO_OPTION
   :yes-no-cancel JOptionPane/YES_NO_CANCEL_OPTION
   :ok-cancel     JOptionPane/OK_CANCEL_OPTION})

(ui/defattributes
  :option-dialog
  (:title [c _ v])
  (:icon [c _ v]
    (.setIcon (impl c) (util/icon v)))
  (:message [c _ v]
    (.setMessage (impl c) v))
  (:options [c _ v]
    (when-let [t (options-type v)]
      (.setOptionType (impl c) t))))
