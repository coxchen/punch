(ns punch.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [punch.utils :as u]
            [punch.views_common    :as common]
            [punch.views_title     :as title]
            [punch.views_doing     :as doing]
            [punch.views_backlog   :as backlog]
            [punch.views_sui
             :refer [button popup checkbox dropdown radio textarea
                     modal modal-h modal-c modal-c modal-d modal-a]]
            [punch.views_calendar :as cal]))

(defn project-list [] (common/editable-list :projects "Projects" :new-project :remove-project))

(defn version-list [] (common/editable-list :versions "Versions" :new-version :remove-version))

(defn main-panel []
  (let [entries  (re-frame/subscribe [:entries])
        projects (re-frame/subscribe [:projects])
        backlog (re-frame/subscribe  [:backlog])]
    (fn []
      [:div

       [title/title-control]

       [:div.ui.segment.container
        [:h2 [:i.exclamation.circle.icon] "Distractions"]]

       [:div.ui.segment.container
        {:style {:min-height "600px"}}

        [:h2 "Doing (" (count @entries) " )"]

;;          [:> button {:icon "question circle" :class "circular mini teal right floated" :content "sample"
;;                      :on-click #(re-frame/dispatch-sync [:sample-entries])}]]

        [doing/entry-table entries]

        [:div.ui.right.rail
         [cal/week-selection]]

        [:div.ui.left.rail
         [project-list]
         [version-list]]]

       (into
         [:div.ui.segment.container
          [:h2 "Backlog (" (count @backlog) " )"
           [backlog/new-backlog-popup]]]

         (for [[idx b] (map-indexed (fn [idx item] [idx item]) @backlog)]
           [backlog/backlog-entry idx b]))])))

