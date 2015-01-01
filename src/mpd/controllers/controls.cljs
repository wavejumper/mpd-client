(ns mpd.controllers.controls)

(defmulti control-event
  (fn [[event & args] state] event))

(defmethod control-event :default [[event & _] state]
  (.log js/console (str "No control-event for " event))
  state)

(defmethod control-event :change-view [[_ next-view] state]
  (assoc state :view next-view))

(defmethod control-event :play [_ state]
  (assoc-in state [:status :state] "play"))

(defmethod control-event :pause [_ state]
  (update-in state [:status :state]
             #(condp = %
                "pause" "play"
                "stop" "stop"
                "pause")))

(defmethod control-event :stop [_ state]
  (assoc-in state [:status :state] "stop"))

(defmethod control-event :playid [[event _ args] state]
  (-> state
      (assoc-in [:status :songid] (first args))
      (assoc-in [:status :state] "play")))

(defmethod control-event :status [[event data _] state]
  (assoc state :status data))

(defmethod control-event :playlistid [[event data _] state]
  (let [songid (:id data)]
    (assoc-in state [:cache :songid songid] data)))

(defmethod control-event :playlistinfo [[event data _] state]
  (let [songids (->> data
                     (map (fn [x] [(:id x) x]))
                     (into {}))]
    (-> state
        (update-in [:cache :songid] #(merge % songids))
        (assoc :playlist data))))

(defmethod control-event :next [_ state]
  (assoc-in state [:status :songid]
            (get-in state [:status :nextsongid])))

(defmethod control-event :clear [[event & _] state]
  (assoc state :playlist []))
