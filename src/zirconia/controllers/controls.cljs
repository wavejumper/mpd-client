(ns zirconia.controllers.controls
  (:require [plumbing.core :as plumbing :refer-macros (?>)]))

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
        (assoc-in [:cache :playlist] data))))

(defmethod control-event :next [_ state]
  (assoc-in state [:status :songid]
            (get-in state [:status :nextsongid])))

(defmethod control-event :clear [[event & _] state]
  (assoc-in state [:cache :playlist] []))

(defmethod control-event :list [[event data _] state]
  (let [settings (get-in state [:view-settings :list])]
    (assoc-in state [:cache :list settings] data)))

(defmethod control-event :find [[event data _] state]
  (let [songids (->> data
                     (map (fn [x] [(:id x) x]))
                     (into {}))]
    (-> state
        (update-in [:cache :songid] #(merge % songids)))))

(defmethod control-event :lsinfo [[event data _] state]
  (let [songids (->> data
                     (filter :file)
                     (map (fn [x] [(:id x) x]))
                     (into {}))
        uri (get-in state [:view-settings :browse :uri])]
    (-> state
        (update-in [:cache :songid] #(merge % songids))
        (update-in [:cache :lsinfo uri] data))))

(defmethod control-event :idle [[event data _] state]
  (condp = (:changed data)
    "update"
    (assoc state :cache {})

    ;; else
    state))
