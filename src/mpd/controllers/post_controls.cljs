(ns mpd.controllers.post-controls
  (:require [cljs.core.async :as async :refer [<! >!]])
  (:require-macros [cljs.core.async.macros :refer (go go-loop)]))

(extend-type js/RegExp
  cljs.core/IFn
  (-invoke ([this s] (re-matches this s))))

(defn event->command [event]
  (str (name event) "\n"))

(defmulti post-control-event!
  (fn [[event & args] com prev-state state] event))

(defmethod post-control-event! :default [_ _ _ _] nil)

(defmethod post-control-event! :mpd/pause [[event & _] com _ _]
  (go
    (>! (:socket com) (event->command event))
    (.log js/console (pr-str (<! (:socket com))))))

(defmethod post-control-event! :mpd/play [[event & _] com _ _]
  (go
    (>! (:socket com) (event->command :status))))
