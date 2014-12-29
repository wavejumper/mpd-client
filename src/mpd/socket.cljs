(ns mpd.socket
  "core.async over NodeJS net.Socket

   Insparation from https://gist.github.com/eggsby/6102537"
  (:require
   [clojure.string :refer (split)]
   [cljs.core.async.impl.protocols :as proto]
   [cljs.core.async :as async :refer (chan <! >! put! close!
                                           dropping-buffer
                                           sliding-buffer)])
  (:require-macros [cljs.core.async.macros :refer (go go-loop)]))

(def ^:private net (js/require "net"))

(extend-type js/RegExp
  cljs.core/IFn
  (-invoke ([this s] (re-matches this s))))

(defn parse-response [data]
  (let [[k & rest] (split data #":")]
    [(keyword k) (apply str rest)]))

(def filter-ok
  (filter #"^OK.*"))

(def filter-ack
  (filter #"^ACK"))

(def filter-parse-response (comp filter-ok
                                 filter-ack
                                 parse-response))

(defn connect
  "Connects to the specified MPD server

  Returns a core.async channel"
  ([port] (connect port "localhost"))
  ([port host]
     (let [socket (new net.Socket)
           [opener buffer] (repeatedly 2 chan)
           write (chan (dropping-buffer 1))
           read (chan (sliding-buffer 1))
           socket-chan
           (reify
             proto/ReadPort
             (take! [_ fn-handler]
               (proto/take! read fn-handler false))
             proto/WritePort
              (put! [_ val fn-handler]
                (proto/put! write val fn-handler))
              proto/Channel
              (close! [_]
                (do (proto/close! read)
                    (proto/close! write)
                    (.end socket))))]
       (go-loop []
          (.write socket (<! write))
          (recur))

        (go-loop [data ""]
          (let [data (+ data (<! buffer))
                lines (split data #"\n")]
            (condp some lines

              #"^OK MPD.*"
              (recur "")

              #"^ACK"
              (do
                (.log js/console "ACK")
                (>! read [:ack data])
                (recur ""))

              #"^OK$"
              (do
                (.log js/console "OK")
                (>! read [:ok lines])
                (recur ""))

              ;; else
              (recur data))))
        (.on socket "data"
             (fn [data]
               (put! buffer (str data))))
        (.connect socket port host)
        socket-chan)))
