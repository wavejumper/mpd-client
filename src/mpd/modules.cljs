(ns mpd.modules
  "Reusable components"
  (:require [mpd.socket :as socket]
            [mpd.dom :as dom]
            [com.stuartsierra.component :as component]
            [om.core :as om :include-macros true]
            [cljs.core.async :as async :refer (chan <! >! close! pub)])
(:require-macros [cljs.core.async.macros :refer (go-loop alt!)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Socket component

(defrecord Socket [port host]
  component/Lifecycle
  (start [component]
    (assoc component :chan (socket/connect port host)))

  (stop [component]
    (when-let [socket (:chan component)]
      (close! socket))
    (dissoc component :chan)))

(defn new-socket
  [& {:keys [port host] :or {port 6600} :as opts}]
  (map->Socket opts))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; EventBus component

(defrecord EventBus [controls post-controls!]
  component/Lifecycle
  (start [component]
    (let [event-bus (chan)
          state (get-in component [:root-cursor :atom])
          socket (get-in component [:socket :chan])]
      (go-loop []
        (let [options (<! event-bus)
              prev-state @state]
          (swap! state (partial controls options))
          (post-controls! options {:event-bus event-bus :socket socket} prev-state @state)
          (recur)))
      (assoc component :chan event-bus)))

  (stop [component]
    (when-let [event-bus (:chan component)]
      (close! event-bus))
    (dissoc component :chan)))

(defn new-event-bus
  [& {:keys [controls post-controls!] :as opts}]
  (map->EventBus opts))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Scheduler

(defrecord Scheduler [timeout scheduled-fn]
  component/Lifecycle
  (start [component]
    (let [close-ch (chan)]
      (go-loop []
        (scheduled-fn component)
        (alt!
          (async/timeout timeout) ([_] (recur))
          close-ch ([_] nil)))
      (assoc component :close-ch close-ch)))

  (stop [component]
    (when-let [close-ch (:close-ch component)]
      (close! close-ch)
      (dissoc component :close-ch))))

(defn new-scheduler [& {:keys [timeout scheduled-fn] :as opts}]
  (map->Scheduler opts))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defrecord Subscriber [publisher-ch subscriptions topic-fn]
  component/Lifecycle
  (start [component]
    (let [publication-ch (pub (:chan publisher-ch) topic-fn)
          subs (subscriptions component)]
      (doseq [[topic sub-fn] subs]
        (socket/subscribe! publication-ch topic sub-fn))
      (assoc component :publication-ch publication-ch)))

  (stop [component]
    (when-let [publication-ch (:publication-ch component)]
      (close! publication-ch))
    (dissoc component :publication-ch)))

(defn new-subscriber
  [& {:keys [publisher-ch subscriptions topic-fn] :as opts}]
  (map->Subscriber opts))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; KeyBinder

(defrecord KeyBinder [key-bindings]
  component/Lifecycle
  (start [component]
    (assoc component :event (dom/bind-keys! (key-bindings component))))

  (stop [component]
    (when-let [event (:event component)]
      (dom/unbind-keys! event))
    (dissoc component :event)))

(defn new-key-binder
  [& {:keys [key-bindings] :as opts}]
  (map->KeyBinder opts))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; RootCursor component

(defrecord RootCursor [init-val reset?]
  component/Lifecycle
  (start [component]
    (assoc component :atom (atom init-val)))

  (stop [component]
    (if reset?
      component
      (if-let [next-val (:atom component)]
        (assoc component :init-val @next-val)
        component)))

  ISwap
  (-swap! [component f]
    (update-in component [:atom] #(swap! % f))))

(defn new-root-cursor
  [& {:keys [init-val reset?] :or {reset? false} :as opts}]
  (map->RootCursor opts))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; OmRoot component

(defrecord OmRoot [root-component options]
  component/Lifecycle
  (start [component]
    (let [event-bus (get-in component [:event-bus :chan])
          socket (get-in component [:socket :chan])
          root-cursor (get-in component [:root-cursor :atom])
          options (-> options
                      (assoc-in [:shared :event-bus] event-bus)
                      (assoc-in [:shared :socket] socket))]
      (assoc component :om (om/root root-component root-cursor options))))

  (stop [component]
    (when-let [target (:target options)]
      (om/detach-root target))
    (dissoc component :om)))

(defn new-om-root
  [& {:keys [root-component options] :as opts}]
  (map->OmRoot opts))
