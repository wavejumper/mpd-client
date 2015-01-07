(ns zirconia.socket
  "core.async over NodeJS net.Socket"
  (:require
   [clojure.string :refer (split)]
   [cljs.core.async.impl.protocols :as proto]
   [cljs.core.async :as async :refer (chan <! >! put! close! sub buffer)]
   [zirconia.utils :refer [parse-response event->command command->event]])
  (:require-macros [cljs.core.async.macros :refer (go go-loop alt!)]))

(def ^:private net (js/require "net"))

(extend-type js/RegExp
  cljs.core/IFn
  (-invoke ([this s] (re-matches this s))))

(defn- create-socket [port host]
  (let [socket (new net.Socket)]
    (.connect socket port host)
    (.setEncoding socket "utf8")
    socket))

(defn- close-socket [socket]
  (.end socket)
  socket)

;; https://gist.github.com/eggsby/6102537
(defn- socket-chan
  "Creates a bi-directional channel"
  [read-ch write-ch & [{:keys [on-close]}]]
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

;; TODO: handle connect, close, error, etc events
(defn connect
  "Connects to the specified MPD server

  Returns a core.async channel"
  ([port] (connect port "localhost"))
  ([port host]
     (let [socket (create-socket port host)
           ;; TODO: pass transducers into
           ;;       connect fn so that code is more reusable
           process-ch (chan 10 (map parse-response))
           write-ch (chan 10 (map event->command))
           read-ch (chan)
           buffer-ch (chan)
           open-ch (chan)
           socket-ch (socket-chan read-ch
                                  write-ch
                                  {:on-close (fn [] (close-socket socket))})]

       (go-loop [connection false]
         ;; wait until open connection
         (if-not connection
           (do (<! open-ch)
               (recur true))

           ;; process one command at a time
           (let [command (<! write-ch)
                 block-ch (chan)]
             (.write socket command
                     (fn []
                       (go
                         (let [response (<! process-ch)
                               event (command->event command)]
                           (>! read-ch (assoc event :response response))
                           (close! block-ch)))))
             (<! block-ch)
             (recur true))))

       (.on socket "data"
            (fn [data]
              (put! buffer-ch (str data))))

       (.on socket "connect"
            (fn []
              (.log js/console (str "Connected to MPD: " host ":" port))
              (close! open-ch)))

       (.on socket "error"
            (fn [error]
              (.log js/console (pr-str error))))

       (.on socket "close"
            (fn [_]
              (.log js/console "Connection to MPD closed")))

       ;; TODO: abstract this code out of connect fn
       ;;       so that it is more generic/reusable
       (go-loop [data ""]
         (let [data (str data (<! buffer-ch))
               lines (split data #"\n")]
           (condp some lines

             #"^OK MPD.*"
             (do
               (.log js/console data)
               (>! read-ch {:command :ok-mpd
                            :args []
                            :response data})
               (recur ""))

             #"^ACK"
             (do
               (.log js/console (str "ACK: " data))
               (recur ""))

             #"^OK$"
             (do
               (>! process-ch lines)
               (recur ""))

             ;; else
             (recur data))))

       socket-ch)))

;; TODO: make into cool macro?
(defn subscribe! [publication-ch topic sub-fn]
  (let [sub-ch (chan)]
    (sub publication-ch topic sub-ch)
    (go-loop []
      (sub-fn (<! sub-ch))
      (recur))))
