(ns mpd.core
  (:require
   [mpd.socket :as socket]
   [clojure.string :refer [split]]
   [cljs.core.async :as async :refer (chan <! >! put! close!)])
  (:require-macros [cljs.core.async.macros :refer (go go-loop)]))

(extend-type js/RegExp
  cljs.core/IFn
  (-invoke ([this s] (re-matches this s))))

(def socket-chan (socket/connect 6600))
(def event-bus (chan))

(go-loop []
  (let [command (<! event-bus)]
    (>! socket-chan (str (name command) "\n"))
    (loop [data ""]
      (let [data (+ data (<! socket-chan))
            lines (split data #"\n")]
        (condp some lines
          #"^ACK"
          (.log js/console "ACK")

          #"^OK MPD.*"
          (.log js/console "OK MPD!")

          #"^OK$"
          (.log js/console "OK")

          ;; else
          (recur data))))
    (recur)))

(aset js/document "onclick"
      (fn []
        (.log js/console "Yes?")
        (put! event-bus :status)))
