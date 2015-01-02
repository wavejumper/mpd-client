(ns zirconia.controllers.post-controls
  (:require [cljs.core.async :as async :refer [<! >! put!]])
  (:require-macros [cljs.core.async.macros :refer (go go-loop)]))

(defmulti post-control-event!
  (fn [[event & args] owner prev-state state] event))

(defmethod post-control-event! :default [[event & _] _ _ _]
  nil)

(defmethod post-control-event! :change-view [[_ next-view] owner _ state]
  (let [view-settings (get-in state [:view-settings :list])]
    (condp = next-view
      :list
      (when (empty? (get-in state [:cache :list view-settings]))
        (let [socket (get-in owner [:socket :chan])
              show (get-in state [:view-settings :list :show])
              group (get-in state [:view-settings :list :group])]
          (put! socket {:command :list
                        :args [show "group" group]})))

      ;; else
      nil)))

(defmethod post-control-event! :status [[event data _] owner _ state]
  (let [socket (get-in owner [:socket :chan])
        songid (:songid data)
        nextsongid (:nextsongid data)]
    (when (and songid (not (get-in state [:cache :songid songid])))
      (put! socket {:command :playlistid :args [songid]}))
    (when (and nextsongid (not (get-in state [:cache :songid nextsongid])))
      (put! socket {:command :playlistid :args [nextsongid]}))))

(defmethod post-control-event! :idle [[event data _] owner _ state]
  (let [idle-socket (get-in owner [:idle-socket :chan])
        socket (get-in owner [:socket :chan])]
    (condp = (:changed data)
      "update"
      (let [show (get-in state [:view-settings :list :show])
            group (get-in state [:view-setings :list :group])]
        (put! socket {:command :playlistinfo})
        (put! socket {:command :status})
        (put! socket {:command :list
                      :args [show "group" group]}))

      ;; else
      nil)
    (put! idle-socket {:command :idle})))
