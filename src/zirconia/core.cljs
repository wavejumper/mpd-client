(ns zirconia.core
  (:require
   [com.stuartsierra.component :as component]
   [zirconia.modules :as modules]
   [zirconia.hooks.schedules :as schedules]
   [zirconia.hooks.subscriptions :refer (subscription-service idle-subscription-service)]
   [zirconia.hooks.bindings :refer (key-service)]
   [zirconia.controllers.controls :refer (control-event)]
   [zirconia.controllers.post-controls :refer (post-control-event!)]
   [zirconia.components.app :as app]
   [cljs.core.async :as async]))

(defonce state
  {:status {}
   :cache {}
   :view :playlist
   :views #{:playlist :list :browse}
   :view-settings {:list {:show "album" :group "artist"}
                   :browse {:uri "/"}}})

(defn new-system
  [& {:keys [port host target state]}]
  (-> (component/system-map
       ;; MPD connections
       :socket (modules/new-socket :port port
                                   :host host)
       :idle-socket (modules/new-socket :port port
                                        :host host)
       ;; OM + App state
       :root-cursor (modules/new-root-cursor :init-val state)
       :om (modules/new-om-root :root-component app/root
                                :options {:target target})

       ;; Subscriptions to MPD
       :subscriber
       (modules/new-subscriber :subscriptions subscription-service
                               :topic-fn #(:command %))
       :idle-subscriber
       (modules/new-subscriber :subscriptions idle-subscription-service
                               :topic-fn #(:command %))

       ;; Controller
       :event-bus (modules/new-event-bus :controls control-event
                                         :post-controls! post-control-event!)

       ;; Key bindings
       :key-binder (modules/new-key-binder :key-bindings key-service)

       ;; Schedules
       :status-poll
       (modules/new-scheduler :timeout 1000
                              :scheduled-fn schedules/check-status)
       :playlist-poll
       (modules/new-scheduler :timeout 5000
                              :scheduled-fn schedules/get-playlist)
       :idle-poll
       (modules/new-scheduler :timeout nil
                              :scheduled-fn schedules/idle))

      ;; System dependencies
      (component/system-using
       {:om {:root-cursor :root-cursor
             :event-bus :event-bus
             :socket :socket}
        :subscriber {:event-bus :event-bus
                     :publisher-ch :socket}
        :idle-subscriber {:event-bus :event-bus
                          :publisher-ch :idle-socket}
        :event-bus {:root-cursor :root-cursor
                    :socket :socket
                    :idle-socket :idle-socket}
        :key-binder {:socket :socket
                     :event-bus :event-bus}
        ;; Schedules
        :idle-poll {:socket :idle-socket}
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
