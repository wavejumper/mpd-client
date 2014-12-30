(ns mpd.controllers.post-controls
  (:require [cljs.core.async :as async :refer [<! >! put!]])
  (:require-macros [cljs.core.async.macros :refer (go go-loop)]))

(defmulti post-control-event!
  (fn [[event & args] com prev-state state] event))

(defmethod post-control-event! :default [[event & _] com _ _]
  (let [socket (:socket com)]
    (put! socket event)))

(defmethod post-control-event! :mpd/status [[event & _] com _ _]
  (let [socket (:socket com)
        event-bus (:event-bus com)]
    (go
      (>! socket event)
      (.log js/console (pr-str (<! socket))))))
