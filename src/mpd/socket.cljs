(ns mpd.socket
  "core.async over NodeJS net.Socket"
  (:require
   [clojure.string :refer (split)]
   [cljs.core.async.impl.protocols :as proto]
   [cljs.core.async :as async :refer (chan <! >! put! close!
                                           dropping-buffer
                                           sliding-buffer)])
  (:require-macros [cljs.core.async.macros :refer (go go-loop alt!)]))

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

(defn event->command [event]
  (str (name event) "\n"))

(defn- create-socket [port host]
  (let [socket (new net.Socket)]
    (.connect socket port host)
    (.setEncoding socket "utf8")
    socket))

(defn- close-socket [socket]
  (.end socket)
  socket)

(defn- socket-chan [read-ch write-ch & [{:keys [on-close]}]]
  (reify
    proto/ReadPort
    (take! [_ fn-handler]
      (proto/take! read-ch fn-handler))
    proto/WritePort
    (put! [_ val fn-handler]
      (proto/put! write-ch val fn-handler))
    proto/Channel
    (close! [_]
      (do (proto/close! read-ch)
          (proto/close! write-ch)
          (when on-close
            (on-close))))))

(defn connect
  "Connects to the specified MPD server

  Returns a core.async channel"
  ([port] (connect port "localhost"))
  ([port host]
     (let [socket (create-socket port host)
           read-ch (chan (sliding-buffer 1))
           write-ch (chan (dropping-buffer 1) (map event->command))
           buffer-ch (chan)
           socket-ch (socket-chan read-ch
                                  write-ch
                                  {:on-close (fn [] close-socket socket)})]

       (go-loop []
         (.write socket (<! write-ch))
         (recur))

       (.on socket "data"
            (fn [data] (put! buffer-ch (str data))))

       (go-loop [data ""]
         (let [data (+ data (<! buffer-ch))
               lines (split data #"\n")]
           (condp some lines

             #"^OK MPD.*"
             (recur "")

             #"^ACK"
             (do (>! read-ch [:ack data])
                 (recur ""))

             #"^OK$"
             (do (>! read-ch [:ok lines])
                 (recur ""))

             ;; else
             (recur data))))

       socket-ch)))
