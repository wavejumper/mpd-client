(ns mpd.utils
  (:require [clojure.string :refer (split trim join lower-case)]
            [cljs.reader :as reader]))

(extend-type js/RegExp
  cljs.core/IFn
  (-invoke ([this s] (re-matches this s))))

;; TODO: less hacky
(defn- read-string* [x]
  (try (.parse js/JSON x)
       (catch js/Error _ (str x))))

(defn camel->lisp [x]
  (-> (name x)
      (.replace #"([a-z])([A-Z])" "$1-$2")
      lower-case
      keyword))

(defn parse-line [data]
  (let [[k & rest] (split data #":")]
    (when-let [k (keyword k)]
      [(camel->lisp k)
       (->> rest (join ":") trim read-string*)])))

(defn parse-response [data]
  (->> data
       (remove #"^ACK")
       (remove #"^OK.*")
       (map parse-line)
       ;;(map clojureize-key)
       (into {})))

(defn event->command [{:keys [command args]}]
  (str (name command) " " (join " " args) "\n"))

(defn command->event [data]
  (let [[command & args] (-> data (split #"\n") first trim (split #" "))]
    {:command (keyword command)
     :args (map read-string* args)}))
