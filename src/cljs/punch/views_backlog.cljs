(ns punch.views_backlog
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [goog.string :as gstring]
            [punch.utils :as u]
            [punch.views_common    :as common]
            [punch.views_doing     :as doing]
            [punch.views_sui
             :refer [button popup checkbox dropdown radio textarea
                     modal modal-h modal-c modal-c modal-d modal-a]]))


(defn new-backlog-form []
  (let [versions (re-frame/subscribe [:versions])
        projects (re-frame/subscribe [:projects])]
    (r/with-let [topic    (r/atom "")
                 jira     (r/atom "")
                 er?      (r/atom false)
                 version  (r/atom nil)
                 project  (r/atom nil)
                 comments (r/atom nil)]
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
        [:> textarea {:rows 2 :value @comments
                      :on-change #(reset! comments (-> % .-target .-value))}]]
       [:hr]
       [:> button {:icon "plus" :class "circular mini"
                   :on-click (fn [_]
                               (let [entry {:topic @topic
                                            :jira  @jira
                                            :er?   @er?
                                            :version (if @version (nth @versions @version))
                                            :project (if @project (nth @projects @project))
                                            :comment @comments}]
                                 (re-frame/dispatch-sync [:add-backlog entry])))}]])))

(defn new-backlog-popup []
  (let [is-popup-open (re-frame/subscribe [:is-backlog-popup-open])]
    [:> popup
     {:trigger (r/as-element [:> button {:icon "meh" :class "violet circular mini right floated"
                                         :content "new backlog"}])
      :flowing true
      :on "click"
      :open @is-popup-open
      :onOpen  #(re-frame/dispatch-sync [:open-backlog-popup])
      :onClose #(re-frame/dispatch-sync [:close-backlog-popup])}
     [new-backlog-form]]))


(defn backlog-entry [idx b]
  (fn []
    [:div.ui.raised.segment
     [doing/entry-ribbon b]
     [:> popup
       {:trigger (r/as-element [:h4.backlog-entry (:topic b) " @ " (:project b)])
        :flowing true :hoverable true}
      [:div
        [:> button {:icon "thumbs up" :class "circular mini twitter" :content "I'm doing"
                    :on-click #(re-frame/dispatch-sync [:move-backlog-to-doing idx b])}]
        [:> button {:icon "trash"  :class "circular mini red" :content "remove"
                    :on-click #(re-frame/dispatch-sync [:remove-backlog idx b])}]]]
     [:p (:comment b)]]))


