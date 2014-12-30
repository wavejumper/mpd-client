(ns mpd.core
  (:require
   [com.stuartsierra.component :as component]
   [mpd.modules :as modules]
   [mpd.schedules :as schedules]
   [mpd.controllers.controls :refer [control-event]]
   [mpd.controllers.post-controls :refer [post-control-event!]]
   [mpd.components.app :as app]
   [cljs.core.async :as async]))

(defonce state {:foo :bar})

(defn new-system
  [& {:keys [port host target state]}]
  (-> (component/system-map
       :socket (modules/new-socket :port port
                                   :host host)
       :root-cursor (modules/new-root-cursor :init-val state)
       :om (modules/new-om-root :root-component app/root
                                :options {:target target})
       :scheduler (modules/new-scheduler :timeout 1000
                                         :scheduled-fn schedules/check-status)
       :event-bus (modules/new-event-bus :controls control-event
                                         :post-controls! post-control-event!))
      (component/system-using
       {:om {:root-cursor :root-cursor
             :event-bus :event-bus}
        :scheduler {:event-bus :event-bus}
        :event-bus {:root-cursor :root-cursor
                    :socket :socket}})))

(def ^:dynamic system
  (new-system :port 6600
              :target (. js/document (getElementById "app"))
              :state state))

(set! system (component/start system))
