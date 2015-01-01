(ns mpd.controllers.post-controls
  (:require [cljs.core.async :as async :refer [<! >! put!]])
  (:require-macros [cljs.core.async.macros :refer (go go-loop)]))

(defmulti post-control-event!
  (fn [[event & args] com prev-state state] event))

(defmethod post-control-event! :default [[event & _] com _ _]
  nil)

(defmethod post-control-event! :status [[event data _] com _ state]
  (let [event-bus (:event-bus com)
        socket (:socket com)
        songid (:songid data)
        nextsongid (:nextsongid data)]
    (when (and songid (not (get-in state [:cache :songid songid])))
      (put! socket {:command :playlistid :args [songid]}))
    (when (and nextsongid (not (get-in state [:cache :songid nextsongid])))
      (put! socket {:command :playlistid :args [nextsongid]}))))
