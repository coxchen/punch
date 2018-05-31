(ns punch.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :username
 (fn [db]
   (:username db)))

(re-frame/reg-sub
 :versions
 (fn [db]
   (:versions db)))

(re-frame/reg-sub
 :projects
 (fn [db]
   (:projects db)))

(re-frame/reg-sub
 :actions
 (fn [db]
   (:actions db)))

(re-frame/reg-sub
 :week-days
 (fn [db]
   (:week-days db)))

(re-frame/reg-sub
 :is-entry-popup-open
 (fn [db]
   (:is-entry-popup-open db)))

(re-frame/reg-sub
 :is-backlog-popup-open
 (fn [db]
   (:is-backlog-popup-open db)))

(re-frame/reg-sub
 :entries
 (fn [db]
   (:entries db)))

(re-frame/reg-sub
 :backlog
 (fn [db]
   (:backlog db)))

(re-frame/reg-sub
 :weekdate
 (fn [db]
   (:weekdate db)))
