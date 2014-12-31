(ns mpd.schedules
  (:require [cljs.core.async :as async]))

(defn check-status [owner]
  (let [socket (get-in owner [:socket :chan])]
    (async/put! socket {:command :status})))
