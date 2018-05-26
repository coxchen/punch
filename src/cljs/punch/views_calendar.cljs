(ns punch.views_calendar
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [goog.object]
            [punch.utils :as u]
            [cljsjs.react-date-range]))

(def date-range js/ReactDateRange)

(def DatePicker (goog.object/get date-range "Calendar"))

(defn week-range [day]
  {:startDate (-> (.startOf day "week") (.add 1 "days")      (.format) (js/moment))
   :endDate   (-> (.endOf   day "week") (.subtract 1 "days") (.format) (js/moment))})

(defn week-selection []
  (let [weekdate (re-frame/subscribe [:weekdate])]
    (fn []
      [:> DatePicker
       {:range (-> (or @weekdate (u/this-week-date))
                   (u/day->moment)
                   (week-range))
        :on-change (fn [m]
                     (re-frame/dispatch-sync [:update-weekdate (u/moment->week-date m)]))}])))
