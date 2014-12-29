(ns mpd.socket
  (:require
   [cljs.core.async.impl.protocols :as proto]
   [cljs.core.async :as async :refer (chan <! >! put! close!)])
  (:require-macros [cljs.core.async.macros :refer (go go-loop)]))

(def ^:private net (js/require "net"))

(defn connect
  "net.Socket as a core.async channel
  returns a channel which delivers the socket chan then closes

  Based on https://gist.github.com/eggsby/6102537"
  ([port] (connect port "localhost"))
  ([port host]
     (let [socket (new net.Socket)
           [read write opener] (repeatedly 3 chan)
           socket-chan
           (reify
             proto/ReadPort
             (take! [_ fn-handler]
               (proto/take! read fn-handler))
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
        (.on socket "data"
             (fn [data]
               (put! read (str data))))
        (.on socket "connect"
             (fn []
               (go (>! opener socket-chan) (close! opener))))
        (.connect socket port host)
        socket-chan)))
