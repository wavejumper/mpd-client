(ns mpd.components.app
  (:require [om.core :as om]
            [om-tools.core :refer-macros [defcomponentk]]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :as async]))

(defcomponentk root
  "Root component of application"
  [[:data status cache :as app] owner [:shared event-bus]]

  (render
   [_]
   (html
    [:div
     [:pre (pr-str cache)]
     [:pre (pr-str status)]
     (when-let [song (get-in cache [:songid (:songid status)])]
       [:div (str "Now playing: " (:artist song) " - " (:title song))])
     [:div {:on-click #(async/put! event-bus [:mpd/play])}
      "Play!!"]

     [:div {:on-click #(async/put! event-bus [:mpd/pause])}
      "Pause"]])))
