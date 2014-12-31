(ns mpd.controllers.subscriptions
  (:require [cljs.core.async :as async :refer (put!)]))

(defn subscription-service [owner]
  (let [event-bus (get-in owner [:event-bus :chan])
        ->event-bus (fn [{:keys [command response]}]
                      (put! event-bus [command response]))]
    {:status ->event-bus
     :playlistid ->event-bus}))
