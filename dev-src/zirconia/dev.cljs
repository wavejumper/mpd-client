(ns zirconia.dev
  (:require
   [figwheel.client :as fw]
   [weasel.repl :as ws-repl]
   [zirconia.core :as zirconia]))

(fw/start
 {:websocket-url "ws://localhost:3449/figwheel-ws"
  :on-jsload
  (fn []
    (.log js/console "Got something new from browser")
    (zirconia/reset-system!))})

(ws-repl/connect "ws://localhost:9001")
