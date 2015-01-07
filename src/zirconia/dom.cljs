(ns zirconia.dom
  (:require [goog.events :as events]))

(defn bind-keys! [key-bindings]
  (letfn [(listen-fn [e]
            (.log js/console "KEY: " (.-keyCode e))
            (when-let [event-fn (get key-bindings (.-keyCode e))]
              (event-fn e)))]
    (events/listen js/document goog.events.EventType.KEYUP listen-fn)))

(defn unbind-keys! [event]
  (events/unlisten event))
