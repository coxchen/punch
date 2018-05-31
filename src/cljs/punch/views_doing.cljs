(ns punch.views_doing
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [goog.string :as gstring]
            [punch.utils :as u]
            [punch.views_common    :as common]
            [punch.views_reporting :as reporting]
            [punch.views_sui
             :refer [button popup checkbox dropdown radio textarea
                     modal modal-h modal-c modal-c modal-d modal-a]]))

;;;;;;;;;;;;;;;;;;;;
;; new entry

(def empty-log {:mon [] :tue [] :wed [] :thr [] :fri []})

(defn new-entry-form []
  (let [versions (re-frame/subscribe [:versions])
        projects (re-frame/subscribe [:projects])]
    (r/with-let [topic   (r/atom "")
                 jira    (r/atom "")
                 er?     (r/atom false)
                 version (r/atom nil)
                 project (r/atom nil)]
      [:div.ui.form
       (common/text-input-inline topic "Topic"  true)
       (common/text-input-inline jira  "JIRA #" false
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
                     :options (common/dropdown-options @versions)
                     :value @version
                     :on-change (fn [e d]
                                  (reset! version (.-value d))
                                  (.log js/console (.-value d)))}]

       [:> dropdown {:placeholder "project ?"
                     :search true :selection true :fluid true
                     :options (common/dropdown-options @projects)
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



;;;;;;;;;;;;;;;;;;;;
;; display entry

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
    [:> button {:icon "thumbs down" :class "circular mini twitter" :content "to backlog"
                :on-click #(re-frame/dispatch-sync [:copy-doing-to-backlog idx entry])}]
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


;;;;;;;;;;;;;;;;;;;;
;; display entry table

(defn entry-table [entries]
  (fn []
    [:table {:class "ui celled padded definition table" :style {:width "100%"}}
     [:thead [:tr [:th] [:th :mon] [:th :tue] [:th :wed] [:th :thr] [:th :fri]]]

     (into [:tbody]
           (for [e (map-indexed (fn [idx itm] [idx itm]) @entries)]
             [entry-row e]))

     [:tfoot {:class"full-width"}
      [:tr
       [:th {:colSpan "6"}

        [new-entry-popup]

        [reporting/reporting-modal]

        [:> button {:icon "trash" :class "circular mini black right floated" :content "clear"
                    :on-click #(re-frame/dispatch-sync [:clear-entries])}]]]]]))
