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

(defn backup? [backup-time change-time entries backlog]
  (cond
    (and (nil? @backup-time)
         (or (> (count @entries) 0)
             (> (count @backlog) 0))) false
    (nil? @change-time) true
    :else
    (.isAfter (u/datetime->moment @backup-time)
              (u/datetime->moment @change-time))))

(defn backup-indicator []
  (let [change-time  (re-frame/subscribe [:change-time])
        backup-time  (re-frame/subscribe [:backup-time])
        entries  (re-frame/subscribe [:entries])
        backlog  (re-frame/subscribe [:backlog])]
    [:div.ui.label
     {:class (if (backup? backup-time change-time
                          entries backlog)
               "olive right corner"
               "red right corner")}
     [:i {:class (if (backup? backup-time change-time
                              entries backlog)
                   "check circle icon"
                   "exclamation circle icon")}]]))

(defn main-panel []
  (let [entries  (re-frame/subscribe [:entries])
        projects (re-frame/subscribe [:projects])
        backlog  (re-frame/subscribe [:backlog])]
    (fn []
      [:div.ui.grid.centered

       [:div.row
        [:div.column.eight.wide
         [title/title-control]
         [:div.ui.segment.container
          [:h2 [:i.exclamation.circle.icon] "Distractions"]]]]


       [:div.row
        [:div.column.three.wide
         [project-list]
         [version-list]]

        [:div.column.eight.wide
         [:div.ui.segment.container
          {:style {:min-height "600px"}}

          [:h2 "Doing (" (count @entries) ")"]
          [backup-indicator]

          [doing/entry-table entries]

          (into
            [:div.ui.segment.container
             [:h2 "Backlog (" (count @backlog) " )"
              [backlog/new-backlog-popup]]]

            (for [[idx b] (map-indexed (fn [idx item] [idx item]) @backlog)]
              [backlog/backlog-entry idx b]))]]

        [:div.column.three.wide
         [cal/week-selection]]]])))
