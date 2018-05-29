(ns punch.views_title
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [goog.string :as gstring]
            [punch.views_common    :as common]
            [punch.views_sui
             :refer [button popup checkbox dropdown radio textarea
                     modal modal-h modal-c modal-c modal-d modal-a]]))

(defn login-form [username secret]
  [:div.ui.form
   [:div.inline.fields
    [common/text-input username "User Name" true]
    [common/text-input secret   "Secret String" true "password"]
    [:> button {:icon "user" :class "circular mini olive"
                :content "login"
                :on-click #(re-frame/dispatch-sync [:login {:username @username
                                                            :secret @secret}])}]]])

(defn title-control []
  (let [login-username (re-frame/subscribe [:username])]
    (r/with-let [expand-control? (r/atom false)
                 username (r/atom "")
                 secret   (r/atom "")]
      [:div.ui.segment.container
       {:class (if @expand-control? "smallheadline expand" "smallheadline")}

       [:h1 "PUNCH"
        [:> button {:icon (if @expand-control? "chevron circle up" "chevron circle down")
                    :class "circular mini twitter right floated"
                    :content @login-username
                    :on-click #(reset! expand-control? (not @expand-control?))}]]

       (if (empty? @login-username)
         [login-form username secret]
         [:div.ui.label
          [:i.user.icon] @login-username
          [:i.delete.icon
           {:on-click #(re-frame/dispatch-sync [:logout])}]])

       [:hr]
       [:span (gstring/unescapeEntities "&nbsp;")]

       [:> button {:icon "low vision" :class "circular mini orange right floated"
                   :content "clear local storage"
                   :on-click #(re-frame/dispatch-sync [:clear-local-storage])}]

       [:> button {:icon "cloud upload" :class "circular mini olive right floated"
                   :content "backup"
                   :on-click #(re-frame/dispatch-sync [:backup])}]])))
