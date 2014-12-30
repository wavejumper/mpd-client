(ns mpd.components.app
  (:require [om.core :as om]
            [om-tools.core :refer-macros [defcomponentk]]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :as async]))

(defcomponentk root
  "Root component of application"
  [[:data status :as app] owner [:shared event-bus]]

  (render
   [_]
   (html
    [:div
     [:pre (pr-str status)]
     [:div {:on-click #(async/put! event-bus [:mpd/play])}
      "Play"]

     [:div {:on-click #(async/put! event-bus [:mpd/pause])}
      "Pause"]])))
