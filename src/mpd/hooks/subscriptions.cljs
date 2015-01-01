(ns mpd.hooks.subscriptions
  (:require [cljs.core.async :as async :refer (put!)]))

(defn subscription-service [owner]
  (let [event-bus (get-in owner [:event-bus :chan])
        ->event-bus (fn [{:keys [command args response]}]
                      (put! event-bus [command response args]))]
    {:status ->event-bus
     :playlistid ->event-bus
     :playlistinfo ->event-bus
     :playid ->event-bus
     :play ->event-bus
     :pause ->event-bus}))
