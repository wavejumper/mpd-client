(ns mpd.hooks.bindings
  (:require [cljs.core.async :as async :refer (put!)]))

(defn key-service [owner]
  (let [socket (get-in owner [:socket :chan])
        event-bus (get-in owner [:event-bus :chan])]
    {80 ;; [p]ause
     (fn [_] (put! socket {:command :pause}))}))
