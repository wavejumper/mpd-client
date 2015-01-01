(ns zirconia.repl
  (:require [weasel.repl.websocket]
            [cemerick.piggieback :as pback]))

(defn browser-repl
  "Boots up a cljs repl via piggieback and weasel"
  [& {:keys [ip port] :or {ip "0.0.0.0" port 9001}}]
  (cemerick.piggieback/cljs-repl
   :repl-env (weasel.repl.websocket/repl-env :ip ip :port port)))

(defn cljs-repl
  "Boots up a cljs repl via piggieback"
  []
  (cemerick.piggieback/cljs-repl))
