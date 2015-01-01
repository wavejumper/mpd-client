(ns mpd.controllers.controls)

(defmulti control-event
  (fn [[event & args] state] event))

(defmethod control-event :default [_ state] state)

(defmethod control-event :play [_ state]
  (assoc-in state [:status :state] "play"))

(defmethod control-event :pause [_ state]
  (assoc-in state [:status :state] "pause"))

(defmethod control-event :status [[event data] state]
  (assoc state :status data))

(defmethod control-event :playlistid [[event data] state]
  (let [songid (:id data)]
    (assoc-in state [:cache :songid songid] data)))

(defmethod control-event :playlistinfo [[event data] state]
  (let [songids (->> data
                     (map (fn [x] [(:id x) x]))
                     (into {}))]
    (-> state
        (update-in [:cache :songid] #(merge % songids))
        (assoc :playlist data))))
