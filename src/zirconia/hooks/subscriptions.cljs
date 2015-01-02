(ns zirconia.hooks.subscriptions
  "Topics to be subscribed and initialized by the Subscriber component"
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
     :stop ->event-bus
     :pause ->event-bus
     :clear ->event-bus
     :list ->event-bus
     :find ->event-bus
     :lsinfo ->event-bus
     :next ->event-bus}))

(defn idle-subscription-service [owner]
  (let [event-bus (get-in owner [:event-bus :chan])
        ->event-bus (fn [{:keys [command args response]}]
                      (put! event-bus [command response args]))]
    {:idle ->event-bus}))
