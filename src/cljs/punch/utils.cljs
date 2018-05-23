(ns punch.utils
  (:require [cljsjs.moment]))

(def date-format "YYYY-MMDD")
(def week-format "YYYYWW")
(def datetime-format "YYYY-MMDD HH:mm:ss")

(defn day->moment  [d] (-> d (js/moment date-format)))
(defn moment->day  [m] (-> m (.format date-format)))
(defn moment->datetime  [m] (-> m (.format datetime-format)))
(defn moment->week-date [m] (-> m (.startOf "isoWeek") (.format date-format)))
(defn moment->year-week-num [m] (-> m (.format week-format) int))
(defn today [] (moment->day (js/moment)))
(defn this-moment [] (js/moment))

(defn compare-week [op]
  (fn [date]
    (if-not (empty? date)
      (op (-> date (day->moment) (moment->year-week-num))
          (-> (this-moment) (moment->year-week-num))))))

(defn before-this-week? [date] ((compare-week <) date))
(defn within-this-week? [date] ((compare-week =) date))
