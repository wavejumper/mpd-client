(ns mpd.hooks.schedules
  (:require [cljs.core.async :as async :refer (put!)]))

(defn check-status [owner]
  (let [socket (get-in owner [:socket :chan])]
    (put! socket {:command :status})))

(defn get-playlist [owner]
  (let [socket (get-in owner [:socket :chan])]
    (put! socket {:command :playlistinfo})))
