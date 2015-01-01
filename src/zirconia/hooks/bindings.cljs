(ns zirconia.hooks.bindings
  "Keybindings to be initialized by the KeyBinder component"
  (:require [cljs.core.async :as async :refer (put!)]))

(defn key-service [owner]
  (let [socket (get-in owner [:socket :chan])
        event-bus (get-in owner [:event-bus :chan])]
    {80 ;; [p]ause
     (fn [_] (put! socket {:command :pause}))
     85 ;; [u]pdate
     (fn [_] (put! socket {:command :update}))
     83 ;; [s]top
     (fn [_] (put! socket {:command :stop}))
     190 ;; >
     (fn [_] (put! socket {:command :next}))
     188 ;; <
     (fn [_] (put! socket {:command :previous}))
     67 ;; [c]lear
     (fn [_] (put! socket {:command :clear}))}))
