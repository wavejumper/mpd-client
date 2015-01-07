(ns zirconia.components.app
  (:require [om.core :as om]
            [om-tools.core :refer-macros (defcomponentk)]
            [sablono.core :as html :refer-macros (html)]
            [cljs.core.async :as async :refer (put!)]
            [zirconia.utils :refer (perc ms->minute)]))

(defcomponentk controls
  [[:data status song :as app]
   [:shared socket]]

  (render
   [_]
   (let [state (:state status)]
     (html
      [:div
       (if (= "play" state)
         [:div {:on-click #(async/put! socket {:command :pause})}
          "Pause"]

         [:div {:on-click #(async/put! socket {:command :play})}
          "Play!!"])

       [:div {:on-click #(async/put! socket {:command :previous})}
        "< "]

       [:div {:on-click #(async/put! socket {:command :next})}
        "> "]

       (when song
         [:div
          [:div (str "Now playing: " (:artist song) " - " (:title song))]
          [:div
           "[" (ms->minute (:elapsed status)) "/"
           (ms->minute (:time song)) "]"]
          [:div
           {:style {:background-color "pink"
                    :height "10px"
                    :width (str (perc (:elapsed status) (:time song)) "%")}}]])]))))

(defcomponentk playlist
  [[:data playingid playlist :as app]
   [:shared socket]]
  (render
   [_]
   (html
    [:ul {:key "playlist"}
     (for [{:keys [id artist title album track]} playlist
           :let [row (str track " - " artist " - " title " - " album)]]
       [:li
        {:on-click #(put! socket {:command :playid :args [id]})
         :key (str "playlist-" id)}
        (if (= id playingid)
          [:strong row]
          [:span row])])])))

(defcomponentk list-view
  [[:data results settings :as app]
   [:shared socket]]

  (render
   [_]
   (let [group (keyword (:group settings))
         show (keyword (:show settings))]
     (html
      [:div
       [:pre (pr-str settings)]
       [:ul {:key "browse"}
        (for [result (sort-by group results)
              :when (not (empty? (show result)))]
          [:li (group result) " - " (show result)])]]))))

(defcomponentk view-tabs
  [[:data view views :as app]
   [:shared event-bus]]

  (render
   [_]
   (html
    [:ul {:key "view-tabs"}
     (for [v views]
       [:li
        {:key (str "view-" (name v))
         :on-click #(put! event-bus [:change-view v])}
        (if (= view v)
          [:strong (name v)]
          [:span (name v)])])])))

(defcomponentk root
  "Root component of application"
  [[:data view views view-settings status cache :as app]
   [:shared event-bus]]

  (render
   [_]
   (let [state (:state status)
         playingid (when (not= "stop" state) (:songid status))]
     (html
      [:div
       [:pre (pr-str status)]

       (->view-tabs {:view view :views views})

       (condp = view
         :playlist
         (->playlist {:playlist (:playlist cache)
                      :playingid playingid})

         :list
         (->list-view {:results (get-in cache [:list (:list view-settings)])
                       :settings (:list view-settings)})

         ;; else
         [:div "No such view " (str view)])

       (->controls {:status status
                    :song (get-in cache [:songid playingid])})]))))
