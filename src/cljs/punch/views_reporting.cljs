(ns punch.views_reporting
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [goog.string :as gstring]
            [punch.views_sui
             :refer [button popup checkbox dropdown radio textarea
                     modal modal-h modal-c modal-c modal-d modal-a]]))

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
                      [:li l])))
          ])]))

(defn reporting-projects [group-by-ver ver]
  (let [group-by-proj (group-by :project (get group-by-ver ver))
        _ (.log js/console "group-by-proj" (pr-str group-by-proj))]
    (into [:div [:h3 (or ver "UNKNOWN_VERSION")]]
          (for [[idx p] (map-indexed (fn [idx itm] [idx itm]) (keys group-by-proj))]
            ^{:key idx} [:div
                         (if (and (not= p "None") (not-empty p)) [:h4 "[project] " p])
                         [reporting-entries group-by-proj p]]))))

(defn default-item []
 [:ul [:li [:topic "None"]]])

(defn reporting-items [items versions title]
  (let [_ (.log js/console (pr-str (group-by :version @items)))
        group-by-ver (group-by :version @items)]
    (into [:div
            [:h3 "--------------------" title "--------------------"]]
          (or (not-empty (for [[idx ver] (map-indexed (fn [idx itm] [idx itm]) (keys group-by-ver))]
                          ^{:key idx} [reporting-projects group-by-ver ver]))
              [[default-item]])
    )))

(defn reporting-problem []
  [:div
    [:h3 "--------------------Problem--------------------"]
    [:ul [:li [:topic "None"]]]])

(defn reporting []
  (let [entries  (re-frame/subscribe [:entries])
        projects (re-frame/subscribe [:projects])
        versions (re-frame/subscribe [:versions])
        backlog (re-frame/subscribe  [:backlog])]
    [:div
      [reporting-items entries versions "Progress"]
      [reporting-items backlog versions "Plan"]
      [reporting-problem]
    ]
  )
)


(defn reporting-modal []
  [:> modal {:trigger (r/as-element [:> button {:icon "list" :class "violet circular mini" :content "report"}])
             :style {:margin-top "10px" :display "block"}}
   [:> modal-h "Weekly Report"]
   [:> modal-c
    [:> modal-d
     [reporting]]]])
