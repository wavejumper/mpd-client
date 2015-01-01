(ns zirconia.core
  (:require
   [com.stuartsierra.component :as component]
   [zirconia.modules :as modules]
   [zirconia.hooks.schedules :as schedules]
   [zirconia.hooks.subscriptions :refer (subscription-service)]
   [zirconia.hooks.bindings :refer (key-service)]
   [zirconia.controllers.controls :refer (control-event)]
   [zirconia.controllers.post-controls :refer (post-control-event!)]
   [zirconia.components.app :as app]
   [cljs.core.async :as async]))

(defonce state {:status {}
                :cache {}
                :playlist []
                :view :playlist
                :view-settings {:playlist {}
                                :browse {:browse-by :album}}})

(defn new-system
  [& {:keys [port host target state]}]
  (-> (component/system-map
       :socket (modules/new-socket :port port
                                   :host host)
       :root-cursor (modules/new-root-cursor :init-val state)
       :om (modules/new-om-root :root-component app/root
                                :options {:target target})
       :subscriber (modules/new-subscriber :subscriptions subscription-service
                                           :topic-fn #(:command %))
       :event-bus (modules/new-event-bus :controls control-event
                                         :post-controls! post-control-event!)
       :key-binder (modules/new-key-binder :key-bindings key-service)
       ;; Schedules
       :status-poll
       (modules/new-scheduler :timeout 1000
                              :scheduled-fn schedules/check-status)
       :playlist-poll
       (modules/new-scheduler :timeout 5000
                              :scheduled-fn schedules/get-playlist))
      (component/system-using
       {:om {:root-cursor :root-cursor
             :event-bus :event-bus
             :socket :socket}
        :subscriber {:event-bus :event-bus
                     :publisher-ch :socket}
        :event-bus {:root-cursor :root-cursor
                    :socket :socket}
        :key-binder {:socket :socket
                     :event-bus :event-bus}
        ;; Schedules
        :playlist-poll {:socket :socket}
        :status-poll {:socket :socket}})))

(def ^:dynamic system
  (new-system :port 6600
              :target (. js/document (getElementById "app"))
              :state state))

(defn start-system! []
  (set! system (component/start system)))

(defn stop-system! []
  (set! system (component/stop system)))

(defn reset-system! []
  (when system
    (stop-system!))
  (start-system!))

(start-system!)
