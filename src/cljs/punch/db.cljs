(ns punch.db)

(def default-db
  {:name "PUNCH"
   :versions ["1.0"]

   :projects ["None"
              "Punch"]

   :actions {:check       {:idx 1 :icon "eye"       :text "check"}
             :discuss     {:idx 2 :icon "comments"  :text "discuss"}
             :fix-or-impl {:idx 3 :icon "ambulance" :text "fix-or-impl"}
             :doc         {:idx 4 :icon "edit"      :text "doc"}}

   :week-days ["mon" "tue" "wed" "thr" "fri"]

   :is-entry-popup-open false
   :is-backlog-popup-open false

   :entries []

   :backlog []

   :done []

   :samples [
              {:topic "Put Punch on GitHub"
               :jira nil
               :version "1.0"
               :project "My Side Project"
               :logs {:mon ["doc"]
                      :tue []
                      :wed []
                      :thr []
                      :fri []}}]})

