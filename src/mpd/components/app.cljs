(ns mpd.components.app
  (:require [om.core :as om]
            [om-tools.core :refer-macros [defcomponentk]]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :as async]))

(defcomponentk root
  "Root component of application"
  [data owner [:shared event-bus]]

  (render [_]
          (.log js/console "Rendering")
   (html
    [:div "Hello world!"])))
