(ns punch.events
  (:require [re-frame.core :as re-frame]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [reagent.crypt :as crypt]
            [punch.db :as db]
            [punch.utils :as u]))

;; localStorage
(defn ->json-str [obj] (js/JSON.stringify (clj->js obj)))
(defn store [k obj] (.setItem    js/localStorage k (js/JSON.stringify (clj->js obj))))
(defn clear [k]     (.removeItem js/localStorage k))

(defn keywordify [m]
  (cond
    (map? m) (into {} (for [[k v] m] [(keyword k) (keywordify v)]))
    (coll? m) (vec (map keywordify m))
    :else m))

(defn fetch [k default]
  (let [item (.getItem js/localStorage k)]
    (if item
      (-> (.getItem js/localStorage k)
          (or (js-obj))
          (js/JSON.parse)
          (js->clj)
          (keywordify))
      default)))

(def storage-db "worklog-db")

;; Events

(defn vec-remove
  "remove elem in coll"
  [coll pos]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))

(re-frame/reg-fx
 :log
 (fn [msg] (.log js/console (pr-str msg))))

(re-frame/reg-fx
 :save-db
 (fn [the-db] (store storage-db the-db)))

(re-frame/reg-fx
 :clear-db
 (fn [] (clear storage-db)))

(re-frame/reg-event-fx
 :initialize-db
 (fn  [_ _]
   (let [stored-db (fetch storage-db db/default-db)
         initialized-db
         (-> db/default-db
             (assoc :entries  (:entries  stored-db))
             (assoc :backlog  (:backlog  stored-db))
             (assoc :projects (:projects stored-db))
             (assoc :versions (:versions stored-db))
             (assoc :username (:username stored-db))
             (assoc :secret   (:secret   stored-db))
             (assoc :weekdate (:weekdate stored-db))
             (assoc :backup-time (:backup-time stored-db))
             (assoc :change-time (:change-time stored-db)))]
     {:log [:initialize-db]
      :db initialized-db
      :save-db initialized-db})))

(defn remove-cred [db] (dissoc db :username :secret))

(re-frame/reg-event-fx
  :login
  (fn [cofx [_ cred]]
    (let [db   (:db cofx)
          cred (assoc cred :secret (crypt/hash (:secret cred) :sha256 true))]
      {:log [:login cred]
       :db  (merge db cred)
       :http-xhrio {:method          :post
                    :uri             "/login"
                    :params          cred
                    :format          (ajax/json-request-format)
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [:on-login-resp]
                    :on-failure      [:on-login-failed]}})))

(re-frame/reg-event-fx
  :on-login-resp
  (fn [cofx [_ result]]
    (let [{:keys [login created]} result
          success? (or login created)
          db (if-not success?
               (remove-cred (:db cofx))
               (:db cofx))]
      {:log [:on-login-resp "success?" success?]
       :db  db
       :save-db db})))

(re-frame/reg-event-fx
  :on-login-failed
  (fn [cofx [_ result]]
    (let [db (remove-cred (:db cofx))]
      {:log [:on-login-failed result]
       :db  db
       :save-db db})))

(re-frame/reg-event-fx
  :logout
  (fn [cofx [_ result]]
    (let [user (get-in cofx [:db :username])
          db   (remove-cred (:db cofx))]
      {:log [:logout user]
       :db  db
       :save-db db})))

(re-frame/reg-event-fx
  :update-weekdate
  (fn [cofx [_ weekdate]]
    (let [db (assoc (:db cofx) :weekdate weekdate)]
      {:log [:update-weekdate weekdate]
       :db  db
       :save-db db})))

(def to-backup-keys [:versions :projects :entries :backlog :backup-time :change-time])

(defn stamp-weekdate [db]
  (assoc db :weekdate (-> (u/this-moment) (u/moment->week-date))))

(defn stamp-backup [db]
  (assoc db :backup-time (-> (u/this-moment) (u/moment->datetime))))

(defn stamp-change [db]
  (assoc db :change-time (-> (u/this-moment) (u/moment->datetime))))

(re-frame/reg-event-fx
  :backup
  (fn [cofx _]
    (let [to-backup {:content  (->json-str (select-keys (:db cofx) to-backup-keys))
                     :weekdate (or (:weekdate (:db cofx))
                                   (u/this-week-date))
                     :username (:username (:db cofx))
                     :secret   (:secret (:db cofx))}]
      {:log [:backup to-backup]
       :http-xhrio {:method          :post
                    :uri             "/backup"
                    :params          to-backup
                    :format          (ajax/json-request-format)
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [:on-backup-resp]
                    :on-failure      [:on-backup-failed]}})))

(re-frame/reg-event-fx
  :on-backup-resp
  (fn [cofx [_ result]]
    (let [stamp-db (stamp-backup (:db cofx))]
      {:log [:on-backup-resp "result" result]
       :db  stamp-db
       :save-db stamp-db})))

(re-frame/reg-event-fx
  :on-backup-failed
  (fn [cofx [_ result]]
    (let [a 1]
      {:log [:on-backup-failed result]})))
;;        :db  db
;;        :save-db db})))


;;;;;;;;;;;;;;;;;;;;
;; doing entry

(re-frame/reg-event-fx
  :add-entry
  (fn [cofx [_ entry]]
    (let [timed-entry (assoc entry :added (u/today))
          db (-> (:db cofx)
                 (update-in [:entries] #(conj % timed-entry))
                 (stamp-change))]
      {:log [:add-entry (pr-str [(u/today) timed-entry])]
       :db  db
       :save-db db
       :dispatch [:close-entry-popup]})))

(re-frame/reg-event-fx
  :clear-entries
  (fn [cofx _]
    (let [db (assoc-in (:db cofx) [:entries] [])]
      {:log [:clear-entries]
       :db  db
       :save-db db})))

(re-frame/reg-event-fx
  :clear-local-storage
  (fn [cofx _]
    {:log [:clear-local-storage]
     :clear-db nil}))

(re-frame/reg-event-fx
  :sample-entries
  (fn [cofx _]
    (let [db (assoc-in (:db cofx) [:entries] (get-in cofx [:db :samples]))]
      {:log [:sample-entries]
       :db  db
       :save-db db})))

(re-frame/reg-event-fx
  :post-action
  (fn [cofx [_ entryid weekday action action-comment]]
    (let [wd (keyword weekday)
          db (-> (:db cofx)
                 (update-in [:entries entryid :logs wd]
                            #(conj % (if (not-empty action-comment)
                                       [(:text action) action-comment]
                                       (:text action))))
                 (stamp-change))
          actions (get-in db [:entries entryid :logs wd])]
      {:log (str "entry "   entryid " | "
                 "weekday " (pr-str wd) " | "
                 "action "  (:text action) " -> "
                 actions " | "
                 "comment -> " action-comment)
       :db db
       :save-db db})))

(re-frame/reg-event-fx
  :remove-action
  (fn [cofx [_ entry-idx day action-idx]]
    (let [db (-> (:db cofx)
                 (update-in [:entries entry-idx :logs (keyword day)] #(vec-remove % action-idx))
                 (stamp-change))]
      {:log [:remove-action entry-idx day action-idx]
       :db  db
       :save-db db})))

(re-frame/reg-event-fx
  :open-entry-popup
  (fn [cofx _]
    (let [db (assoc-in (:db cofx) [:is-entry-popup-open] true)]
      {:log [:open-entry-popup]
       :db  db})))

(re-frame/reg-event-fx
  :close-entry-popup
  (fn [cofx _]
    (let [db (assoc-in (:db cofx) [:is-entry-popup-open] false)]
      {:log [:close-entry-popup]
       :db  db
       :save-db db})))

(re-frame/reg-event-fx
  :remove-entry
  (fn [cofx [_ idx entry]]
    (let [db (-> (:db cofx)
                 (update-in [:entries] #(vec-remove % idx))
                 (stamp-change))]
      {:log [:remove-entry idx]
       :db  db
       :save-db db})))

(re-frame/reg-event-fx
  :done-entry
  (fn [cofx [_ idx entry]]
    (let [db (update-in (:db cofx) [:entries] #(vec-remove % idx))]
      {:log [:done-entry idx]
       :db  db})))
;;        :save-db db})))

;;;;;;;;;;;;;;;;;;;;
;; backlog

(re-frame/reg-event-fx
  :add-backlog
  (fn [cofx [_ entry]]
    (let [db (-> (:db cofx)
                 (update-in [:backlog] #(conj % entry))
                 (stamp-change))]
      {:log [:add-backlog entry]
       :db  db
       :save-db db
       :dispatch [:close-backlog-popup]})))

(re-frame/reg-event-fx
  :remove-backlog
  (fn [cofx [_ idx entry]]
    (let [db (-> (:db cofx)
                 (update-in [:backlog] #(vec-remove % idx))
                 (stamp-change))]
      {:log [:remove-backlog idx]
       :db  db
       :save-db db})))

(re-frame/reg-event-fx
  :open-backlog-popup
  (fn [cofx _]
    (let [db (assoc-in (:db cofx) [:is-backlog-popup-open] true)]
      {:log [:open-backlog-popup]
       :db  db})))

(re-frame/reg-event-fx
  :close-backlog-popup
  (fn [cofx _]
    (let [db (assoc-in (:db cofx) [:is-backlog-popup-open] false)]
      {:log [:close-backlog-popup]
       :db  db
       :save-db db})))

(re-frame/reg-event-fx
  :move-backlog-to-doing
  (fn [cofx [_ idx entry]]
    (let [timed-entry (assoc entry :added (u/today))
          db (-> (:db cofx)
                 (update-in [:backlog] #(vec-remove % idx))
                 (update-in [:entries] #(conj % timed-entry))
                 (stamp-change))]
      {:log [:move-backlog-to-doing timed-entry]
       :db  db
       :save-db db})))

(re-frame/reg-event-fx
  :copy-doing-to-backlog
  (fn [cofx [_ idx entry]]
    (let [db (-> (:db cofx)
                 ; (update-in [:entries] #(vec-remove % idx))
                 (update-in [:backlog] #(conj % entry))
                 (stamp-change))]
      {:log [:move-doing-to-backlog entry]
       :db  db
       :save-db db})))

;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx
  :new-project
  (fn [cofx [_ proj]]
    (let [db (-> (:db cofx)
                 (update-in [:projects] #(conj % proj))
                 (stamp-change))]
      {:log [:new-project (pr-str proj)]
       :db  db
       :save-db db})))

(re-frame/reg-event-fx
  :remove-project
  (fn [cofx [_ idx]]
    (let [db (-> (:db cofx)
                 (update-in [:projects] #(vec-remove % idx))
                 (stamp-change))]
      {:log [:remove-project (pr-str idx)]
       :db  db
       :save-db db})))

(re-frame/reg-event-fx
  :new-version
  (fn [cofx [_ proj]]
    (let [db (-> (:db cofx)
                 (update-in [:versions] #(conj % proj))
                 (stamp-change))]
      {:log [:new-version (pr-str proj)]
       :db  db
       :save-db db})))

(re-frame/reg-event-fx
  :remove-version
  (fn [cofx [_ idx]]
    (let [db (-> (:db cofx)
                 (update-in [:versions] #(vec-remove % idx))
                 (stamp-change))]
      {:log [:remove-version (pr-str idx)]
       :db  db
       :save-db db})))
