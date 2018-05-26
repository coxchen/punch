(ns punch.views_sui
  (:require [cljsjs.semantic-ui-react]
            [goog.object]))

(def semantic-ui js/semanticUIReact)

(defn sui-component
  "Get a component from sematic-ui-react:

    (component \"Button\")
    (component \"Menu\" \"Item\")"
  [k & ks]
  (if (seq ks)
    (apply goog.object/getValueByKeys semantic-ui k ks)
    (goog.object/get semantic-ui k)))

(def button   (sui-component "Button"))
(def grid     (sui-component "Grid"))
(def column   (sui-component "Grid" "Column"))
(def popup    (sui-component "Popup"))
(def rating   (sui-component "Rating"))
(def checkbox (sui-component "Checkbox"))
(def dropdown (sui-component "Dropdown"))
(def radio    (sui-component "Radio"))
(def textarea (sui-component "TextArea"))

(def modal   (sui-component "Modal"))
(def modal-h (sui-component "Modal" "Header"))
(def modal-c (sui-component "Modal" "Content"))
(def modal-d (sui-component "Modal" "Description"))
(def modal-a (sui-component "Modal" "Actions"))
