(ns mpd.controllers.controls)

(defmulti control-event
  (fn [[event & args] state] event))

(defmethod control-event :default [_ state] state)

(defmethod control-event :status [[event data] state]
  (assoc state :status data))

(defmethod control-event :playlistid [[event data] state]
  (let [songid (:id data)]
    (assoc-in state [:cache :songid songid] data)))
