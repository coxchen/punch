(ns punch.db
  (:require [environ.core :refer [env]]
            [clojure.java.jdbc :as j]
            [yesql.core :refer [defquery defqueries]]))

(defqueries "sql/queries.sql")
