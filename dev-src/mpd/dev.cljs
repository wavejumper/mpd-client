(ns mpd.dev
  (:require
   [com.stuartsierra.component :as component]
   [figwheel.client :as fw]
   [weasel.repl :as ws-repl]
   [mpd.core :as mpd]))

(defn reset-system! []
  (when mpd/system
    (set! mpd/system (component/stop mpd/system)))
  (set! mpd/system (component/start mpd/system)))

(fw/start
 {:websocket-url "ws://localhost:3449/figwheel-ws"
  :on-jsload (fn [] (.log js/console "Got something new from browser")
                (reset-system!))})

(ws-repl/connect "ws://localhost:9001")
