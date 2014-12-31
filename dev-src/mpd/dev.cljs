(ns mpd.dev
  (:require
   [figwheel.client :as fw]
   [weasel.repl :as ws-repl]
   [mpd.core :as mpd]))

(fw/start
 {:websocket-url "ws://localhost:3449/figwheel-ws"
  :on-jsload
  (fn []
    (.log js/console "Got something new from browser")
    (mpd/reset-system!))})

(ws-repl/connect "ws://localhost:9001")
