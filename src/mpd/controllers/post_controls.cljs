(ns mpd.controllers.post-controls)

(defmulti post-control-event!
  (fn [[event & args] com prev-state state] event))

(defmethod post-control-event! :default [_ _ _ _] nil)
