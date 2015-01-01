(ns mpd.components.app
  (:require [om.core :as om]
            [om-tools.core :refer-macros (defcomponentk)]
            [sablono.core :as html :refer-macros (html)]
            [cljs.core.async :as async :refer (put!)]))

(defcomponentk controls
  [[:data state :as app] [:shared socket]]

  (render
   [_]
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
      "> "]])))

(defcomponentk playlist
  [[:data playingid playlist :as app] [:shared socket]]
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

(defcomponentk root
  "Root component of application"
  [[:data status playlist cache :as app] owner]

  (render
   [_]
   (let [playingid (:songid status)]
     (html
      [:div
       [:pre (pr-str cache)]
       [:pre (pr-str status)]

       (when-let [song (get-in cache [:songid playingid])]
         [:div (str "Now playing: " (:artist song) " - " (:title song))])

       (->playlist {:playlist playlist
                    :playingid playingid})

       (->controls {:state (:state status)})]))))
