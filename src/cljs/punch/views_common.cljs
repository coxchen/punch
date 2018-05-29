(ns punch.views_common
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [punch.views_sui
             :refer [button popup checkbox dropdown radio textarea
                     modal modal-h modal-c modal-c modal-d modal-a]]))

(defn input-valid-class [value required?]
  (if (and required? (empty? @value))
    "field error"
    "field"))

(defn text-input
  ([value placeholder required?] (text-input value placeholder required? "text"))
  ([value placeholder required? input-type]
   [:div {:class (input-valid-class value required?)}
    [:input {:type input-type :value @value :placeholder placeholder
             :on-change #(reset! value (-> % .-target .-value))}]]))

(defn text-input-inline
  ([value placeholder] (text-input-inline value placeholder false nil))
  ([value placeholder required?] (text-input-inline value placeholder required? nil))
  ([value placeholder required? cb]
   [:div.inline.fields
    [:div {:class (input-valid-class value required?)}
     [:label placeholder]
     [:input {:type "text" :value @value :placeholder (if required? "required")
              :on-change #(do
                            (reset! value (-> % .-target .-value))
                            (if cb (cb value)))}]]]))

(defn dropdown-options [alist] (map-indexed (fn [idx item] {:value idx :text item}) alist))

(defn editable-list [items-sub-key title add-event remove-event]
  (let [items (re-frame/subscribe [items-sub-key])]
    (r/with-let [item (r/atom "")
                 editing? (r/atom false)]
      [:div.ui.segment
       [:h3 title
        [:> button {:icon "edit" :class (str "circular mini right floated " (if @editing? "orange" "olive"))
                    :content (if @editing? "editing ..." "edit")
                    :on-click #(reset! editing? (not @editing?))}]]

       (into [:div.ui.raised.segments]
             (for [[i p] (map-indexed (fn [i p] [i p]) @items)]
               [:div.ui.segment
                (if @editing? [:> button {:icon "minus" :class "circular mini right floated red"
                                          :on-click #(re-frame/dispatch-sync [remove-event i])}])
                [:p p]]))

       (if @editing?
         [:div.ui.form
          (text-input-inline item "new?" true)
          [:> button {:icon "plus" :class "circular mini twitter"
                      :on-click #(if (not-empty @item)
                                   (do
                                     (re-frame/dispatch-sync [add-event @item])
                                     (reset! item "")))}]])])))
