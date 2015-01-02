(ns zirconia.utils
  (:require [clojure.string :refer (split trim join lower-case)]
            [cljs.reader :as reader]))

(extend-type js/RegExp
  cljs.core/IFn
  (-invoke ([this s] (re-matches this s))))

;; TODO: less hacky
(defn- read-string* [x]
  (try (.parse js/JSON x)
       (catch js/Error _ (str x))))

(defn camel->lisp
  "Transforms a camelCase string into a lisp-case keyword"
  [x]
  (-> (name x)
      (.replace #"([a-z])([A-Z])" "$1-$2")
      (.replace #"_" "-")
      lower-case
      keyword))

(defn parse-line
  "Parses a single line from a response into a tuple [key value]

  Transforms the keys from camelCase to lisp-case

  eg:
  Foo: bar

  = [:foo bar]"
  [line]
  (let [[k & rest] (split line #":")]
    (when (keyword k)
      [(camel->lisp k)
       (->> rest (join ":") trim read-string*)])))

(defn response->edn
  "Transforms a response from server into EDN friendly format

  Returns a collection if there are duplicate keys in response
  If there are no duplicate keys then a hash-map is returned"
  [data]
  (let [keys (map first data)]
    (if (not= (count keys) (count (distinct keys)))
      (loop [data data next-col [] x []]
        (if (empty? data)
          (conj x (into {} next-col))
          (if (some #{(ffirst data)} (map first next-col))
            (recur (rest data)
                   [(first data)]
                   (conj x (into {} next-col)))
            (recur (rest data)
                   (conj next-col (first data))
                   x))))

      (into {} data))))

(defn parse-response
  "Parses a response from the MPD server"
  [lines]
  (->> lines
       (remove #"^ACK")
       (remove #"^OK.*")
       (map parse-line)
       (response->edn)))

(defn event->command
  "Transforms an event hash-map with keys :command, :args into a raw request string"
  [{:keys [command args]}]
  (str (name command) " " (join " " args) "\n"))

(defn command->event
  "Transforms a raw request string back into its hash-map equivalent"
  [data]
  (let [[command & args] (-> data (split #"\n") first trim (split #" "))]
    {:command (keyword command)
     :args (map read-string* args)}))

(defn perc [x y]
  (.ceil js/Math (* (/ x y) 100)))

(defn pad
  "Prepends 0 to x, n - x times"
  [x n]
  (let [x (str x)
        x-count (count x)]
    (if (< x-count n)
      (let [padding (- n x-count)]
        (loop [x x i 0]
          (if (= i padding)
            x
            (recur (str "0" x) (inc i)))))
      x)))

(defn ms->minute [x]
  (str (.floor js/Math (/ x 60))
       ":"
       (pad (.floor js/Math (mod x 60)) 2)))
