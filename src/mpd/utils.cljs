(ns mpd.utils
  (:require [clojure.string :refer (split trim join lower-case upper-case)]
            [cljs.reader :as reader]))

(extend-type js/RegExp
  cljs.core/IFn
  (-invoke ([this s] (re-matches this s))))

;; TODO: less hacky
(defn- read-string* [x]
  (try (.parse js/JSON x)
       (catch js/Error _ (str x))))

(defn camel->lisp
  "Converts a camelCase string into a lisp-case keyword"
  [x]
  (-> (name x)
      (.replace #"([a-z])([A-Z])" "$1-$2")
      lower-case
      keyword))

(defn parse-line
  "Parses a single line from a response into a tuple [key value]

  eg:
  Foo: bar

  = [Foo bar]"
  [line]
  (let [[k & rest] (split line #":")]
    (when (keyword k)
      [k (->> rest (join ":") trim read-string*)])))

(defn lower-case?
  "Returns if x is a lower-case character"
  [x]
  (not= (str x) (upper-case (str x))))

(defn response->edn
  "Converts a response from server to EDN format
  Transforms all keys to lisp-case style keywords

  If the response is a list of results then it is delimited by each lower-cased key

  eg:
  file: Foo
  Id: 8
  file: Bar
  Id: 9

  = [{:file Foo :id 8} {:file Bar :id 9}]"
  [data]
  (let [keys (map first data)]
    (if (not= (count keys) (count (distinct keys)))
      (let [data (partition-by (fn [[k _]] (lower-case? (first k))) data)]
        (loop [data data x []]
          (if (empty? data)
            x
            (recur (drop 2 data)
                   (conj x (into {} (map (fn [[k v]] [(camel->lisp k) v])
                                         (apply into (take 2 data)))))))))

      (into {} (map (fn [[k v]] [(camel->lisp k) v]) data)))))

(defn parse-response
  "Parses a response from the MPD server"
  [lines]
  (->> lines
       (remove #"^ACK")
       (remove #"^OK.*")
       (map parse-line)
       (response->edn)))

(defn event->command
  "Converts an event hash-map with keys :command :args into a raw request"
  [{:keys [command args]}]
  (str (name command) " " (join " " args) "\n"))

(defn command->event
  "Converts a raw request back into its hash-map equivalent"
  [data]
  (let [[command & args] (-> data (split #"\n") first trim (split #" "))]
    {:command (keyword command)
     :args (map read-string* args)}))

(defn perc [x y]
  (.ceil js/Math (* (/ x y) 100) ))

(defn pad
  "Prepends 0 to x, n - x times"
  [x n]
  (let [x (str x)]
    (if (< (count x) n)
      (let [padding (- size (count x))]
        (loop [x x i 0]
          (if (= i padding)
            x
            (recur (+ "0" x) (inc i)))))
      x)))

(defn ms->minute [x]
  (str (.floor js/Math (/ x 60))
       ":"
       (pad (.ceil js/Math (mod x 60)) 2)))
