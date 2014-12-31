(ns mpd.socket
  "core.async over NodeJS net.Socket"
  (:require
   [cljs.reader :as reader]
   [clojure.string :refer (split trim join)]
   [cljs.core.async.impl.protocols :as proto]
   [cljs.core.async :as async :refer (chan <! >! put! close! sub buffer)])
  (:require-macros [cljs.core.async.macros :refer (go go-loop alt!)]))

(def ^:private net (js/require "net"))

;; https://gist.github.com/alandipert/2346460
(extend-type js/RegExp
  cljs.core/IFn
  (-invoke ([this s] (re-matches this s))))

(defn parse-line [data]
  (let [[k & rest] (split data #":")]
    (when-let [k (keyword k)]
      ;; TODO: reader?
      [k (str (trim (join ":" rest)))])))

(defn parse-response [data]
  (->> data
       (remove #"^ACK")
       (remove #"^OK.*")
       (map parse-line)
       (into {})))

(defn event->command [{:keys [command args]}]
  (str (name command) " " (join " " args) "\n"))

(defn command->event [data]
  (let [[command & args] (-> data (split #"\n") first trim (split #" "))]
    {:command (keyword command)
     :args (map reader/read-string args)}))

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

;; TODO: handle connect, close, error, etc
(defn connect
  "Connects to the specified MPD server

  Returns a core.async channel"
  ([port] (connect port "localhost"))
  ([port host]
     (let [socket (create-socket port host)
           process-ch (chan 10 (map parse-response))
           write-ch (chan 10 (map event->command))
           read-ch (chan)
           buffer-ch (chan)
           socket-ch (socket-chan read-ch
                                  write-ch
                                  {:on-close (fn [] (close-socket socket))})]

       (go-loop []
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
           (recur)))

       (.on socket "data"
            (fn [data] (put! buffer-ch (str data))))

       (go-loop [data ""]
         (let [data (+ data (<! buffer-ch))
               lines (split data #"\n")]
           (condp some lines

             #"^OK MPD.*"
             (recur "")

             #"^ACK"
             (do
               (.log js/console (pr-str "ERR: " data))
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
