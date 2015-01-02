(defproject zirconia "0.1.0-SNAPSHOT"
  :description "MPD client"
  :url "http://github.com/zirconia"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.7.0-alpha4"]
                 [org.clojure/clojurescript "0.0-2511"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [om "0.8.0-beta5"]
                 [com.facebook/react "0.12.2.1"]
                 [sablono "0.2.22"]
                 [kibu/component "0.2.3-SNAPSHOT"]
                 [prismatic/schema "0.3.3"]
                 [prismatic/om-tools "0.3.9"]
                 [prismatic/plumbing "0.3.5"]
                 [com.andrewmcveigh/cljs-time "0.3.0"]]

  :profiles
  {:dev {:dependencies [[com.cemerick/piggieback "0.1.3"]
                        [com.cemerick/clojurescript.test "0.3.3"]
                        [org.bodil/cljs-noderepl "0.1.11"]
                        [figwheel "0.2.0-SNAPSHOT"]
                        [weasel "0.4.2"]]

         :plugins [[lein-node-webkit-build "0.1.6"]
                   [org.bodil/lein-noderepl "0.1.11"]
                   [lein-cljsbuild "1.0.3"]
                   [lein-figwheel "0.2.0-SNAPSHOT"]]

         :source-paths ["src/" "dev-src/"]

         :repl-options {:init-ns zirconia.repl
                        :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

         :node-webkit-build {:root "resources/public"}

         :figwheel {:http-server-root "public"
                    :server-port 3449
                    :css-dirs ["resources/public/css"]}

         :cljsbuild {:builds [{:id "dev"
                               :source-paths ["src/" "dev-src/"]
                               :compiler {:output-to "resources/public/js/compiled/app.js"
                                          :output-dir "resources/public/js/compiled/out"
                                          :optimizations :none
                                          :source-map true}}

                              {:id "test"
                               :source-paths ["src/"]
                               :compiler {:output-to "target/cljs/testable.js"
                                          :optimizations :whitespace
                                          :pretty-print true}}]
                     :test-commands {"unit-tests" ["node" :runner
                                                   "this.literal_js_was_evaluated=true"
                                                   "target/cljs/testable.js"]}}}})
