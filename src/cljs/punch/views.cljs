(ns punch.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [cljsjs.semantic-ui-react]
            [goog.object]
            [goog.string :as gstring]
            [punch.utils :as u]))

(def semantic-ui js/semanticUIReact)

(defn component
  "Get a component from sematic-ui-react:

    (component \"Button\")
    (component \"Menu\" \"Item\")"
  [k & ks]
  (if (seq ks)
    (apply goog.object/getValueByKeys semantic-ui k ks)
    (goog.object/get semantic-ui k)))

(def button   (component "Button"))
(def grid     (component "Grid"))
(def column   (component "Grid" "Column"))
(def popup    (component "Popup"))
(def rating   (component "Rating"))
(def checkbox (component "Checkbox"))
(def dropdown (component "Dropdown"))
(def radio    (component "Radio"))
(def textarea (component "TextArea"))

(def modal   (component "Modal"))
(def modal-h (component "Modal" "Header"))
(def modal-c (component "Modal" "Content"))
(def modal-d (component "Modal" "Description"))
(def modal-a (component "Modal" "Actions"))

(defn input-valid-class [value required?]
  (if (and required? (empty? @value))
    "field error"
    "field"))

(defn text-input
  ([value placeholder required?] (text-input value placeholder required? "text"))
  ([value placeholder required? input-type]
   [:div {:class (input-valid-class value required?)}
    [:input {:type input-type :value @value :placeholder placeholder
             :on-change #(reset! value (-> % .-target .-value))}]]))

(defn text-input-inline
  ([value placeholder] (text-input-inline value placeholder false nil))
  ([value placeholder required?] (text-input-inline value placeholder required? nil))
  ([value placeholder required? cb]
   [:div.inline.fields
    [:div {:class (input-valid-class value required?)}
     [:label placeholder]
     [:input {:type "text" :value @value :placeholder (if required? "required")
              :on-change #(do
                            (reset! value (-> % .-target .-value))
                            (if cb (cb value)))}]]]))

(def empty-log {:mon [] :tue [] :wed [] :thr [] :fri []})

(defn dropdown-options [alist] (map-indexed (fn [idx item] {:value idx :text item}) alist))

(defn new-entry-form []
  (let [versions (re-frame/subscribe [:versions])
        projects (re-frame/subscribe [:projects])]
    (r/with-let [topic   (r/atom "")
                 jira    (r/atom "")
                 er?     (r/atom false)
                 version (r/atom nil)
                 project (r/atom nil)]
      [:div.ui.form
       (text-input-inline topic "Topic"  true)
       (text-input-inline jira  "JIRA #" false
                          (fn [v] (if (and (gstring/startsWith @v "ER") (not @er?))
                                    (reset! er? true))))

       [:div.inline.fields
        [:div.field
         [:div.label "Urgent?"]
         [:> radio {:toggle true
                    :className "teal"
                    :checked @er?
                    :on-change (fn [_]
                                 (reset! er? (not @er?))
                                 (.log js/console "ER?" @er?))}]]]

       [:> dropdown {:placeholder "version ?"
                     :search true :selection true :fluid true
                     :options (dropdown-options @versions)
                     :value @version
                     :on-change (fn [e d]
                                  (reset! version (.-value d))
                                  (.log js/console (.-value d)))}]

       [:> dropdown {:placeholder "project ?"
                     :search true :selection true :fluid true
                     :options (dropdown-options @projects)
                     :value @project
                     :on-change (fn [e d]
                                  (reset! project (.-value d))
                                  (.log js/console (.-value d)))}]
       [:div.field
        [:label "Comment"]
        [:textarea {:rows 2}]]
       [:hr]
       [:> button {:icon "plus" :class "circular mini"
                   :on-click (fn [_]
                               (let [entry {:topic @topic
                                            :jira  @jira
                                            :er?   @er?
                                            :version (if @version (nth @versions @version))
                                            :project (if @project (nth @projects @project))
                                            :logs empty-log}]
                                 (re-frame/dispatch-sync [:add-entry entry])))}]])))

(defn new-entry-popup []
  (let [is-popup-open (re-frame/subscribe [:is-entry-popup-open])]
    [:> popup
     {:trigger (r/as-element [:> button {:icon "bookmark" :class "olive circular mini" :content "new"}])
      :flowing true
      :on "click"
      :open @is-popup-open
      :onOpen  #(re-frame/dispatch-sync [:open-entry-popup])
      :onClose #(re-frame/dispatch-sync [:close-entry-popup])}
     [new-entry-form]]))


(defn action-buttons [entryid weekday]
  (let [actions (re-frame/subscribe [:actions])]
    (r/with-let [action-comment (r/atom "")]
      [:div.ui.two.column.divided.grid
       [:div.column
        (into [:div {:class "ui vertical labeled icon buttons"}]
            (for [a (vals @actions)]
              [:button.ui.button
               {:on-click #(re-frame/dispatch-sync [:post-action entryid weekday a @action-comment])}
               [:i {:class (str (:icon a) " icon")}] (:text a)]))]
       [:div.column
        [:div.ui.form
         [:div.field
          [:label "comment"]
          [:> textarea {:rows 5 :value @action-comment
                        :on-change #(reset! action-comment (-> % .-target .-value))}]]]]])))

(defn popup-actions [entryid weekday]
  [:> popup
   {:trigger
    (r/as-element
      [:> button {:icon "add" :class "circular mini right floated"
                  :hover true :flowing true}])
    :flowing true
    :hoverable true}
   [action-buttons entryid weekday]])

(defn entry-ribbon [entry]
  (let [elems [(:jira entry) (:version entry)]]
    (if (some not-empty elems)
      [:div.ui.ribbon.label {:class (if (:er? entry) "red" "")} (apply str (interpose " | " (filter not-empty elems)))])))

(defn entry-title [idx entry]
  [:div
   {:style {:width "150px" :white-space "nowrap" :overflow "hidden" :text-overflow "ellipsis"}}
   (str "#" idx " " (:topic entry))])

(defn popup-entry-title [idx entry]
  [:> popup
   {:trigger (r/as-element (entry-title idx entry))
    :flowing true :hoverable true}
   [:div
    [:h4 (:topic entry)]
    [:> button {:icon "check" :class "circular mini twitter" :content "done"}]
    [:> button {:icon "trash" :class "circular mini red" :content "remove"
                :on-click #(re-frame/dispatch-sync [:remove-entry idx entry])}]]])

(defn act-icon-str [act actions]
  (if (string? act)
    (:icon (get @actions (keyword act)))
    (:icon (get @actions (keyword (first act)))))) ;; [act comment]

(defn act-strs [act]
  (if (string? act)
    {:act-text act}
    {:act-text (first act)
     :act-comment (last act)}))

(defn act-icon [act actions entry-idx day action-idx]
  [:> popup
   {:trigger (r/as-element [:> button {:icon (act-icon-str act actions)
                                       :class "teal circular mini left floated"}])
    :flowing true :hoverable true}
   (let [{:keys [act-text act-comment]} (act-strs act)]
     [:div
      [:h3 act-text [:> button {:icon "minus" :class "circular mini right floated red"
                                :on-click #(re-frame/dispatch-sync [:remove-action entry-idx day action-idx])}]]
      [:pre act-comment]])])

(defn entry-front [entry-idx entry]
  [:td {:style {:width "15%"}}
   [entry-ribbon entry]
   [popup-entry-title entry-idx entry]])

(defn entry-class [entry]
  (cond
    (empty? (:added entry)) "warning"
    (u/before-this-week? (:added entry)) "active"
    :else ""))

(defn entry-row [[entry-idx entry]]
  (let [days (re-frame/subscribe [:week-days])
        actions (re-frame/subscribe [:actions])
        logs (:logs entry)]
    (into [:tr {:class (entry-class entry)}
           [entry-front entry-idx entry]]
          ;; extract me!!!
          (for [d @days]
            [:td
             (into [:div]
                   (for [[action-idx act] (map-indexed (fn [idx item] [idx item]) (get logs (keyword d)))]
                     [act-icon act actions entry-idx d action-idx]))
             [popup-actions entry-idx d]]))))

(defn ->logs [logs]
  (flatten (for [d logs] (for [a d] (if (string? a) a (str (first a) " " (gstring/unescapeEntities "&rarr;") " " (last a)))))))

(defn reporting-entries [group-by-proj proj]
  (into [:ul
         (for [[idx entry] (map-indexed (fn [idx itm] [idx itm]) (get group-by-proj proj))]
           ^{:key idx}
           [:li
            (if (not-empty (:jira entry)) (str (:jira entry) ": "))
            (:topic entry)
            (let [logs (vals (:logs entry))
                  _ (.log js/console "??" (pr-str logs))]
              (into [:ul]
                    (for [l (->logs logs)]
                      [:li l])))])]))

(defn reporting-projects [group-by-ver ver]
  (let [group-by-proj (group-by :project (get group-by-ver ver))
        _ (.log js/console "group-by-proj" (pr-str group-by-proj))]
    (into [:div [:h3 (or ver "UNKNOWN_VERSION")]]
          (for [[idx p] (map-indexed (fn [idx itm] [idx itm]) (keys group-by-proj))]
            ^{:key idx} [:div
                         (if (and (not= p "None") (not-empty p)) [:h4 "[project] " p])
                         [reporting-entries group-by-proj p]]))))

(defn reporting []
  (let [entries  (re-frame/subscribe [:entries])
        versions (re-frame/subscribe [:versions])
        _ (.log js/console (pr-str (group-by :version @entries)))
        group-by-ver (group-by :version @entries)]
    (into [:div]
          (for [[idx ver] (map-indexed (fn [idx itm] [idx itm]) (keys group-by-ver))]
            ^{:key idx} [reporting-projects group-by-ver ver]))))


(defn reporting-modal []
  [:> modal {:trigger (r/as-element [:> button {:icon "list" :class "violet circular mini" :content "report"}])
             :style {:margin-top "10px" :display "block"}}
   [:> modal-h "Weekly Report"]
   [:> modal-c
    [:> modal-d
     [reporting]]]])

(defn editable-list [items-sub-key title add-event remove-event]
  (let [items (re-frame/subscribe [items-sub-key])]
    (r/with-let [item (r/atom "")
                 editing? (r/atom false)]
      [:div.ui.segment
       [:h3 title
        [:> button {:icon "edit" :class (str "circular mini right floated " (if @editing? "orange" "olive"))
                    :content (if @editing? "editing ..." "edit")
                    :on-click #(reset! editing? (not @editing?))}]]

       (into [:div.ui.raised.segments]
             (for [[i p] (map-indexed (fn [i p] [i p]) @items)]
               [:div.ui.segment
                (if @editing? [:> button {:icon "minus" :class "circular mini right floated red"
                                          :on-click #(re-frame/dispatch-sync [remove-event i])}])
                [:p p]]))

       (if @editing?
         [:div.ui.form
          (text-input-inline item "new?" true)
          [:> button {:icon "plus" :class "circular mini twitter"
                      :on-click #(if (not-empty @item)
                                   (do
                                     (re-frame/dispatch-sync [add-event @item])
                                     (reset! item "")))}]])])))

(defn project-list [] (editable-list :projects "Projects" :new-project :remove-project))

(defn version-list [] (editable-list :versions "Versions" :new-version :remove-version))

(defn login-form [username secret]
  [:div.ui.form
   [:div.inline.fields
    [text-input username "User Name" true]
    [text-input secret   "Secret String" true "password"]
    [:> button {:icon "user" :class "circular mini olive"
                :content "login"
                :on-click #(re-frame/dispatch-sync [:login {:username @username
                                                            :secret @secret}])}]]])


(defn title-control []
  (let [login-username (re-frame/subscribe [:username])]
    (r/with-let [expand-control? (r/atom false)
                 username (r/atom "")
                 secret   (r/atom "")]
      [:div.ui.segment.container
       {:class (if @expand-control? "smallheadline expand" "smallheadline")}

       [:h1 "PUNCH"
        [:> button {:icon (if @expand-control? "chevron circle up" "chevron circle down")
                    :class "circular mini twitter right floated"
                    :content @login-username
                    :on-click #(reset! expand-control? (not @expand-control?))}]]

;;         (if (not-empty @login-username)
;;           [:> button {:icon "user" :class "circular twitter mini right floated"
;;                       :content @login-username}])]

       (if (empty? @login-username)
         [login-form username secret]
         [:div.ui.label
          [:i.user.icon] @login-username
          [:i.delete.icon
           {:on-click #(re-frame/dispatch-sync [:logout])}]])

       [:hr]
       [:span (gstring/unescapeEntities "&nbsp;")]

       [:> button {:icon "low vision" :class "circular mini orange right floated"
                   :content "clear local storage"
                   :on-click #(re-frame/dispatch-sync [:clear-local-storage])}]

       [:> button {:icon "cloud upload" :class "circular mini olive right floated"
                   :content "backup"
                   :on-click #(re-frame/dispatch-sync [:backup])}]])))

(defn main-panel []
  (let [entries  (re-frame/subscribe [:entries])
        projects (re-frame/subscribe [:projects])]
    (fn []
      [:div

       [title-control]

       [:div.ui.segment.container
        [:h2 [:i.exclamation.circle.icon] "Distractions"]]

       [:div.ui.segment.container
        {:style {:min-height "600px"}}

        [:h2 "Doing"
         [:> button {:icon "question circle" :class "circular mini teal right floated" :content "sample"
                     :on-click #(re-frame/dispatch-sync [:sample-entries])}]]

;;         [:div [:> rating {:icon "star" :maxRating 3}]]

        [:table {:class "ui celled padded definition table" :style {:width "100%"}}
         [:thead [:tr [:th] [:th :mon] [:th :tue] [:th :wed] [:th :thr] [:th :fri]]]

         (into [:tbody]
               (for [e (map-indexed (fn [idx itm] [idx itm]) @entries)]
                 [entry-row e]))

         [:tfoot {:class"full-width"}
          [:tr
           [:th {:colSpan "6"}

            [new-entry-popup]

            [reporting-modal]

            [:> button {:icon "trash" :class "circular mini black right floated" :content "clear"
                        :on-click #(re-frame/dispatch-sync [:clear-entries])}]]]]]

        [:div.ui.right.rail
         [:div.ui.segment
          [:h2 "Done"]]]

        [:div.ui.left.rail
         [project-list]
         [version-list]]]

       [:div.ui.segment.container
        [:h2 "Backlog"]]])))

