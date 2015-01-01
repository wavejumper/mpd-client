(ns mpd.dom
  (:require [goog.events :refer (listen unlisten)]))

(defn bind-keys! [key-bindings]
  (listen js/document goog.events.EventType.KEYUP
          (fn [e]
            (.log js/console "KEY: " (.-keyCode e))
            (when-let [event-fn (get key-bindings (.-keyCode e))]
              (event-fn e)))))

(defn unbind-keys! [event]
  (unlisten event))
