(ns mpd.hooks.bindings
  (:require [cljs.core.async :as async :refer (put!)])
  )

(def key-service
  {80 ;; [p]ause
   (fn [owner]
     (let [socket (get-in owner [:socket :chan])]
       (put! socket {:command :pause})))})
