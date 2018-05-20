(ns punch.events
  (:require [re-frame.core :as re-frame]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [reagent.crypt :as crypt]
            [punch.db :as db]))

;; localStorage
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
             (assoc :projects (:projects stored-db))
             (assoc :versions (:versions stored-db))
             (assoc :username (:username stored-db))
             (assoc :secret   (:secret stored-db)))]
     {:log [:initialize-db]
      :db initialized-db
      :save-db initialized-db})))

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
                    :on-failure      [:on-login-failed]}
         })))

(re-frame/reg-event-fx
  :on-login-resp
  (fn [cofx [_ result]]
    (let [{:keys [login created]} result
          success? (or login created)
          db (if-not success?
               (dissoc (:db cofx) :username :secret)
               (:db cofx))]
      {:log [:on-login-resp "success?" success?]
       :db  db
       :save-db db
       })))

(re-frame/reg-event-fx
  :on-login-failed
  (fn [cofx [_ result]]
    (let [db (:db cofx)]
      {:log [:on-login-failed result]
       })))

(re-frame/reg-event-fx
  :add-entry
  (fn [cofx [_ entry]]
    (let [db (update-in (:db cofx) [:entries] #(conj % entry))]
      {:log [:add-entry (pr-str entry)]
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
          db (update-in (:db cofx) [:entries entryid :logs wd]
                        #(conj % (if (not-empty action-comment)
                                   [(:text action) action-comment]
                                   (:text action))))
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
    (let [db (update-in (:db cofx) [:entries entry-idx :logs (keyword day)] #(vec-remove % action-idx))]
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
    (let [db (update-in (:db cofx) [:entries] #(vec-remove % idx))]
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

(re-frame/reg-event-fx
  :new-project
  (fn [cofx [_ proj]]
    (let [db (update-in (:db cofx) [:projects] #(conj % proj))]
      {:log [:new-project (pr-str proj)]
       :db  db
       :save-db db})))

(re-frame/reg-event-fx
  :remove-project
  (fn [cofx [_ idx]]
    (let [db (update-in (:db cofx) [:projects] #(vec-remove % idx))]
      {:log [:remove-project (pr-str idx)]
       :db  db
       :save-db db})))

(re-frame/reg-event-fx
  :new-version
  (fn [cofx [_ proj]]
    (let [db (update-in (:db cofx) [:versions] #(conj % proj))]
      {:log [:new-version (pr-str proj)]
       :db  db
       :save-db db})))

(re-frame/reg-event-fx
  :remove-version
  (fn [cofx [_ idx]]
    (let [db (update-in (:db cofx) [:versions] #(vec-remove % idx))]
      {:log [:remove-version (pr-str idx)]
       :db  db
       :save-db db})))
