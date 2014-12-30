(ns mpd.schedules
  (:require [cljs.core.async :as async]))

(defn check-status [owner]
  (let [event-bus (get-in owner [:event-bus :chan])]
    (async/put! event-bus [:mpd/status])))
