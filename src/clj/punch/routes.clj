(ns punch.routes
  (:require [clojure.java.io :as io]
            [compojure.core :refer [ANY GET PUT POST DELETE routes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [response content-type charset]]
            [punch.db :as db]
            [environ.core :refer [env]]))

(defn db-spec [] {:connection (env :database-url)})

(defn try-create-user [cred]
  (try (db/create-user cred (db-spec))
    (catch org.postgresql.util.PSQLException e
      (println (.getMessage e))
      '())))

(defn match-or-create [cred]
  (let [matched (db/matched-user cred (db-spec))
        created (if (empty? matched)
                  (try-create-user cred)
                  '())]
    {:login   (not (empty? matched))
     :created (not (empty? created))}))

(defn home-routes [endpoint]
  (routes
   (GET "/" _
     (-> "public/index.html"
         io/resource
         io/input-stream
         response
         (assoc :headers {"Content-Type" "text/html; charset=utf-8"})))

   (GET "/users" _
        (-> (db/users {} (db-spec))
            response))

   (POST "/login" req
         (let [cred (select-keys (:body req) [:username :secret])
               login-result (match-or-create cred)]
           (-> login-result
               response)))

   (POST "/backup" req)

   (resources "/")))
