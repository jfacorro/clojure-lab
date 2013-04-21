(ns macho.ui
  (:refer-clojure :exclude [get set])
  (:require [clojure.reflect :as r]
            [clojure.string :as str]
            [clojure.repl :as repl]
            [clojure.java.io :as io]
            [macho.repl :as mrepl]
            [macho.misc :as misc]
            [macho.ui.swing.core :as ui :reload true]
            [macho.ui.swing.highlighter :as hl :reload true]
            [macho.ui.swing.undo :as undo :reload true]
            [macho.ui.swing.text :as txt]
            [macho.ui.protocols :as proto :reload true]))
;;------------------------------
;; TODO: ask if this is acceptable
(misc/intern-vars 'macho.ui.swing.core)
;;------------------------------
(def app-name "Clojure LAB")
(def new-doc-title "Untitled")
(def icons-paths ["icon-16.png"
                  "icon-32.png"
                  "icon-64.png"])
;;------------------------------
(def current-font
  (atom (ui/font :name "Consolas" :styles [:plain] :size 14)))
;;------------------------------
(def default-dir (atom (ui/get (io/file ".") :canonical-path)))
;;------------------------------
(defn list-methods
  "Lists the methods for the supplied class."
  ([c] (list-methods c ""))
  ([c name]
    (let [members (:members (r/type-reflect c :ancestors true))
          methods (filter #(:return-type %) members)]
      (filter 
        #(or (.contains (str %) name) (empty? name))
        (sort (for [m methods] (:name m)))))))
;;------------------------------
(declare current-path current-txt save-document)
;;------------------------------
(defn eval-in-repl
  "Evaluates the code in the specified repl. Code
  can be a string or a list form.

  args:
    - repl:  the repl where to evaluate the code.
    - code:  string or list with the code to evaluate.
    - echo:  print the code to the repl."
  [repl code & {:keys [echo] :or {echo true}}]
  (let [{console :console repl :process} repl
        cin   (:cin repl)]
    (if echo
      (.println console code)
      (.println console ""))
    (if cin
      (doto cin
        (.write (str code "\n"))
        (.flush))
      (load-string code))))
;;------------------------------
(defn eval-src
  "Evaluates source code."
  [main]
  (let [path   (current-path main)
        txt    (current-txt main)
        sel?   (ui/get txt :selected-text)
        code   (or sel? (proto/text txt))]
    (if (or sel? (nil? path))
      (eval-in-repl (:repl main) code)
      (try
        (save-document main)
        (eval-in-repl (:repl main)
          `(do
            (println "Loaded file" ~path)
            (println (load-file ~path)))
          :echo false)
        (catch Exception ex
          (repl/pst ex)))))
  main)
;;------------------------------
(defn current-txt
  "Gets the current active text control."
  [main]
  (let [tabs   (:tabs main)
        idx    (ui/get tabs :selected-index)]
    (when (<= 0 idx)
      (let [scroll (ui/get tabs :component-at idx)
            pnl    (-> scroll (ui/get :viewport) (ui/get :view))]
        (ui/get pnl :component 0)))))
;;------------------------------
(defn file-path-from-user [title]
  (let [dialog (ui/file-browser @default-dir)
        file   (ui/show dialog title)]
    (when file 
      (reset! default-dir (ui/get file :canonical-path))
      (ui/get file :path))))
;;------------------------------
(defn current-path 
  "Finds the current working tab and shows a 
  file chooser window if it's a new file."
  [main]
  (let [tabs (:tabs main)
        idx  (.getSelectedIndex tabs)]
    (when (<= 0 idx)
      (let [path (.getTitleAt tabs idx)]
        (when (not= path new-doc-title)
          path)))))
;;------------------------------
(defn save-document [main]
  (let [tabs     (:tabs main)
        txt-code (current-txt main)
        path     (or (current-path main) (file-path-from-user "Save"))]
    (when (and txt-code path)
      (spit path (proto/text txt-code))
      (.setTitleAt tabs (.getSelectedIndex tabs) path)))
  main)
;;------------------------------
(defn find-src 
  "Shows the dialog for searching the source
  in the current tab."
  [main]
  (let [txt  (current-txt main)
        s    (str/lower-case (proto/text txt))
        ptrn (ui/input-dialog (:main main) "Find" "Enter search string:")
        lims (when ptrn (misc/find-limits (str/lower-case ptrn) s))]
    (remove-highlight txt)
    (doseq [[a b] lims] (ui/add-highlight txt a (- b a))))
  main)
;;------------------------------
(defn find-doc 
  "Uses the clojure.repl/find-doc function to
  search the documentation for the selected string 
  in the current document."
  ([main]
    (find-doc main false))
  ([main find?]
    (let [txt  (current-txt main)
          slct (.getSelectedText txt)
          sym  (when slct (symbol slct))]
      (cond find?
              (repl/find-doc slct)
            sym 
              (eval `(repl/doc ~sym))))))
;;-------------------------------
(defn update-line-numbers
  "Updates the numbers in the text component
  that contains the line numbers."
  [doc lines]
  (let [pos  (.getLength doc)
        root (.getDefaultRootElement doc)
        n    (.getElementIndex root pos)]
    (ui/queue-action
      #(doto lines
        (.setText (apply str (interpose "\n" (range 1 (+ n 2)))))
        (.updateUI)))))
;;------------------------------
(defn insert-text
  "Inserts the specified text in the document."
  ([txt s]
    (insert-text txt s true))
  ([txt s restore]
    (insert-text txt s restore (.getCaretPosition txt)))
  ([txt s restore pos]
    (let [doc (.getDocument txt)]
      (.insertString doc pos s nil)
      (when restore (.setCaretPosition txt pos)))))
;;------------------------------
(defn find-char
  "Finds the next char in s for which pred is true, 
  starting to look from position cur, in the direction 
  specified by dt (1 or -1)."
  [s cur pred dt]
  (cond (or (neg? cur) (<= (.length s) cur)) -1
        (pred (nth s cur)) cur
        :else (recur s (+ cur dt) pred dt)))
;;------------------------------
(defn insert-tabs 
  "Looks for the first previous \\newline character 
and copies the indenting for the new line."
  [txt]
  (let [pos  (-> (.getCaretPosition txt) dec)
        s    (proto/text txt)
        prev (find-char s pos #(= \newline %) -1)
        end  (find-char s (inc prev) #(not= \space %) 1)
        dt   (dec (- end prev))
        t    (apply str (repeat dt " "))]
    (ui/queue-action #(insert-text txt t false))))
;;------------------------------
(defn handle-tab 
  "Adds tabulating characters in place 
  or at the beggining of each line if there's 
  selected text."
  [txt e & shift?]
  (let [tab    "  "
        pos    (.getCaretPosition txt)
        text   (.getSelectedText txt)]
    (.consume e)
    (if-not text
      (ui/queue-action #(insert-text txt tab false))
      (let [start  (.getSelectionStart txt)
            nl     "\n"
            nltab  (str nl tab)
            [match rplc f]
                   (if shift?
                     [nltab nl #(str/replace % #"^  " "")]
                     [nl nltab #(str tab %)])
            text   (f (str/replace text match rplc))
            end    (+ start (count text))]
        (ui/queue-action #(do (.replaceSelection txt text)
                              (.setSelectionStart txt start)
                              (.setSelectionEnd txt end)))))))
;;------------------------------
(defn input-format
  "Handle automatic insertion and format while typing."
  [txt e]
  (let [actions {(ui/key-stroke \() #(insert-text txt ")")
                 (ui/key-stroke \{) #(insert-text txt "}")
                 (ui/key-stroke \[) #(insert-text txt "]")
                 (ui/key-stroke \") #(insert-text txt "\"")
                 (ui/key-stroke "ENTER") #(insert-tabs txt)
                 (ui/key-stroke "TAB") #(handle-tab txt e)
                 (ui/key-stroke "shift TAB") #(handle-tab txt e true)}]
    (when-let [k (first (filter (partial ui/check-key e) (keys actions)))]
      ((actions k)))))
;;------------------------------
(defn change-font-size [txts e]
  (when (ui/check-key e (ui/key-stroke "control CONTROL"))
    (.consume e)
    (let [font @current-font
          op   (if (neg? (ui/get e :wheel-rotation)) inc #(if (> % 1) (dec %) %))
          size (-> (ui/get font :size) op)]
      (reset! current-font (.deriveFont font (float size)))
      (doseq [txt txts] (ui/set txt :font @current-font)))))
;;------------------------------
(defn match-paren [s pos end delta]
  "Finds the matching delimiter for the specified delimiter."
  (loop [cur  (+ pos delta) 
         acum 0]
    (cond (neg? cur) nil
          (<= (.length s) cur) nil
          (= (nth s pos) (nth s cur))
            (recur (+ cur delta) (inc acum))
          (= (nth s cur) end)
            (if (zero? acum) cur
              (recur (+ cur delta) (dec acum)))
          :else (recur (+ cur delta) acum))))
;;------------------------------
(defn check-paren
  "Checks if the characters in the caret's current
  position is a delimiter and looks for the closing/opening
  delimiter."
  [txt]
  (let [tags (atom [])]
    (fn [e]
      (doseq [tag @tags] (ui/remove-highlight txt tag))
      (let [pos   (dec (.getDot e))
            s     (proto/text txt)
            c     (get-in s [pos])
            delim {\( {:end \), :d 1}, \) {:end \(, :d -1}
                   \{ {:end \}, :d 1}, \} {:end \{, :d -1}
                   \[ {:end \], :d 1}, \] {:end \[, :d -1}}]
        (when-let [{end :end dir :d} (delim c)]
          (when-let [end (match-paren s pos end dir)]
            (reset! tags 
                    (doall (map #(ui/add-highlight txt % 1 (ui/color 192)) [pos end])))))))))
;;------------------------------
(defn new-document
  "Adds a new tab to tabs and sets its title."
  ([main]
    (new-document main new-doc-title))
  ([main title] 
    (new-document main title nil))
  ([main title src]
    (let [tabs       (:tabs main)
          doc        (ui/styled-document)
          txt-code   (ui/text-pane doc)
          undo-mgr   (undo/make-undo-mgr)
          pnl-code   (ui/panel)
          pnl-scroll (ui/scroll pnl-code)
          txt-lines  (ui/text-area)]

      (-> pnl-code
        (ui/set :layout (ui/border-layout))
        (ui/add txt-code))
      
      (ui/set pnl-scroll :row-header-view txt-lines)

      (-> txt-lines
        (ui/set :font @current-font)
        (ui/set :editable false)
        (ui/set :background (ui/color 192)))
        
      ; Undo/redo key events
      (ui/on :key-press txt-code
             (fn [_] (when (.canUndo undo-mgr) (.undo undo-mgr)))
             (ui/key-stroke "ctrl Z"))
      (ui/on :key-press txt-code
             (fn [_] (when (.canRedo undo-mgr) (.redo undo-mgr)))
             (ui/key-stroke "ctrl Y"))

      (ui/on :key-press txt-code #(input-format txt-code %))

      (ui/on :caret-update txt-code (check-paren txt-code))
      
      ;; Load the text all at once
      (when src (ui/set txt-code :text src))

      ; High-light text after code edition.
      (ui/on :change 
             txt-code
             #(future (hl/high-light txt-code)
                      (update-line-numbers doc txt-lines)))
                  
      (ui/queue-action #(do (update-line-numbers doc txt-lines)
                            (hl/high-light txt-code)))
      
      ; Add Undo manager
      (ui/set undo-mgr :limit -1)
      (ui/on :undoable doc #(undo/handle-edit undo-mgr %))

      ; Set the increment for each vertical scroll
      (.. pnl-scroll (getVerticalScrollBar) (setUnitIncrement 16))

      (ui/on :mouse-wheel pnl-scroll #(change-font-size [txt-code txt-lines] %))

      (doto tabs
        (.addTab title pnl-scroll)
        (.setSelectedIndex (dec (.getTabCount tabs))))

      (doto txt-code
        (.setFont @current-font)
        (.setForeground (ui/color 255))
        (.setCaretColor (ui/color 255))
        (.setBackground (ui/color 64))
        (.setCaretPosition 0)
        (.grabFocus))

      main)))
;;------------------------------
(defn open-document
  "Open source file."
  [main]
  (let [path (file-path-from-user "Open")]
    (if path
      (new-document main path (slurp path))
      main)))
;;------------------------------
(defn clear-repl 
  "Deletes the text content in the current repl."
  [main]
  (.setText (:repl main) nil)
  main)
;;------------------------------
(defn close-document
  "Close the current tab."
  [main]
  (let [tabs (:tabs main)
        idx  (ui/get tabs :selected-index)]
    (.removeTabAt tabs idx)
    main))
;;------------------------------
(defn repl-console
  "Creates a repl process for the leinigen project supplied,
  attaches the stdin and stdout to a console and returns it."
  [{:keys [cout cin] :as repl-proc}]
  (let [console (ui/console cout cin #(mrepl/close repl-proc))]
    {:console console :process repl-proc}))
;;------------------------------
(defn add-repl
  "Adds a repl console to the bottom half."
  [{:keys [main tabs repl] :as ui} 
   {:keys [console] :as new-repl}]
  (when repl
    (mrepl/close (:process repl)))
  (let [pane (ui/split tabs console :vertical)]
    (ui/set console :font @current-font)
    (-> pane
      (ui/set :resize-weight 0.8)
      (ui/set :divider-location 0.8))
    (-> main
      ui/remove-all
      (ui/add pane))
    (assoc ui :repl new-repl)))
;;------------------------------
(defn load-project-repl
  [ui]
  (when-let [project-path (file-path-from-user "Project File")]
    (->> project-path 
      mrepl/create-project-repl 
      repl-console 
      (add-repl ui))))
;;------------------------------
(defn load-repl
  [ui]
  (->> (mrepl/create-repl)
    repl-console 
    (add-repl ui)))
;;------------------------------
(def menu-options
  [{:name "File"
    :items [{:name "New" :action new-document :keys "ctrl N"}
            {:name "Open" :action open-document :keys "ctrl O"}
            {:name "Save" :action save-document :keys "ctrl S"}
            {:name "Close" :action close-document :keys "ctrl W"}
            {:separator true}
            {:name "Exit" :action #(do % (System/exit 0)) :keys "alt X"}]}
   {:name "Code"
    :items [{:name "Eval" :action eval-src :keys "ctrl ENTER"}
            {:name "Find" :action find-src :keys "ctrl F"}
            {:name "Find docs" :action #(find-doc % true) :keys "ctrl alt F"}
            {:name "Doc" :action find-doc :keys "alt F"}
            {:name "Clear Log" :action clear-repl :keys "ctrl L"}]}
   {:name "REPL"
    :items [{:name "Clojure" :action load-repl :keys "ctrl R"}
            {:name "Project" :action load-project-repl :keys "ctrl shift R"}]}])
;;------------------------------
(defn ui-process
  "Returns a function that calls (f main) and
  returns the value of this call only if it is not nil,
  otherwise returns main."
  [f]
  (fn [main]
    (or (f main) main)))
;;------------------------------
(defn build-menu
  "Builds the application's menu."
  [main]
  (let [menubar (ui/menu-bar)]
    (doseq [{menu-name :name items :items} menu-options]
      (let [menu (ui/menu menu-name)]
        (ui/add menubar menu)
        (doseq [{item-name :name f :action ks :keys separator :separator} items]
          (let [menu-item (or (and separator (ui/menu-separator))
                              (ui/menu-item item-name))]
            (when (not separator)
              (ui/on :click menu-item #(swap! main (ui-process f)))
              (ui/set menu-item :accelerator (ui/key-stroke ks)))
            (ui/add menu menu-item)))))
    menubar))
;;------------------------------------------
(defn make-main
  "Creates the main window and all
  its controls."
  [name]
  (ui/init)
  (let [main     (ui/frame name)
        tabs     (ui/tabbed-pane)
        ui-main  (atom {:main main :tabs tabs})
        icons    (map (comp ui/image io/resource) icons-paths)]
        
    (-> main
      (ui/set :icon-images icons)
      (ui/set :j-menu-bar (build-menu ui-main))
      (ui/set :size 800 600)
      (ui/maximize)
      (ui/show)
      (ui/add tabs))

    ui-main))
;;------------------------------
