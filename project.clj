(defproject mpd "0.1.0-SNAPSHOT"
  :description "MPD client"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2511"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [om "0.8.0-beta5"]
                 [com.facebook/react "0.12.2.1"]
                 [sablono "0.2.22"]
                 [kibu/component "0.2.3-SNAPSHOT"]

                 ;; dev
                 [com.cemerick/piggieback "0.1.3"]
                 [org.bodil/cljs-noderepl "0.1.11"]
                 [figwheel "0.2.0-SNAPSHOT"]
                 [weasel "0.4.2"]]

  :plugins [[lein-node-webkit-build "0.1.6"]
            [org.bodil/lein-noderepl "0.1.11"]
            [lein-cljsbuild "1.0.3"]
            [lein-figwheel "0.2.0-SNAPSHOT"]]

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :node-webkit-build {:root "resources/public"}

  :figwheel {:http-server-root "public"
             :server-port 3449
             :css-dirs ["resources/public/css"]}

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/"]
                        :compiler {:output-to "resources/public/js/compiled/app.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :optimizations :none
                                   :source-map true}}]})
