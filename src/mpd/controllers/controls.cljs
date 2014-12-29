(ns mpd.controllers.controls)

(defmulti control-event
  (fn [[event & args] state] event))

(defmethod control-event :default [_ state] state)
