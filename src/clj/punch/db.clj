(ns punch.db
  (:require [environ.core :refer [env]]
            [clojure.java.jdbc :as j]
            [yesql.core :refer [defquery defqueries]]))


;; (defn users []
;;   (j/query (java.net.URI. (env :database-url))
;;            ["select username from users"]))

;; (defn matched-user [username secret]
;;   (j/query (java.net.URI. (env :database-url))
;;            ["select username from users where username="]))

(defqueries "sql/queries.sql")
