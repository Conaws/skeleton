(defproject rente "1.0.0"
  :description "A barebones Reagent+Sente app deployable to Heroku. Uses Figwheel (for development build) and Component (lifecycle management)."
  :url "http://enterlab.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.5.3"
  :dependencies [[org.clojure/clojure                     "1.8.0-alpha2" ] ; Latest version before hard-linking
                 [org.clojure/clojurescript               "1.7.189"      ]
                 [org.clojure/core.async                  "0.2.374"      ]
                 [com.stuartsierra/component              "0.3.0"        ]
                 [environ                                 "1.0.1"        ]
                 [ch.qos.logback/logback-classic          "1.1.3"        ]
                 [org.clojure/tools.logging               "0.3.1"        ]

                 [ring/ring-core                          "1.4.0"        ]
                 [ring/ring-defaults                      "0.1.5"        ]
                 [compojure                               "1.4.0"        ]
                 [http-kit                                "2.1.19"       ]

                 [com.taoensso/sente                      "1.6.0"        ]
                 [com.cognitect/transit-clj               "0.8.285"      ]
                 [com.cognitect/transit-cljs              "0.8.232"      ]

                 [reagent                                 "0.5.1"        ]
                 [org.webjars/bootstrap                   "3.3.6"        ]

                 ; ==== DB ====
                   [com.datomic/datomic-free              "0.9.5344"     ] ; Latest (as of 2/1/2016)
                     [org.fressian/fressian               "0.6.5"        ]
                     [org.slf4j/slf4j-api                 "1.7.7"        ]
                     [org.slf4j/slf4j-simple              "1.7.7"        ]
                     [com.datomic/datomic-lucene-core     "3.3.0"        ]
                     [org.hornetq/hornetq-server          "2.3.17.Final" ]
                     [com.h2database/h2                   "1.3.171"      ]
                     [org.apache.tomcat/tomcat-jdbc       "7.0.27"
                       :exclusions [commons-logging/commons-logging]     ]
                     [ch.qos.logback/logback-classic      "1.0.1"        ]
                     [com.google.guava/guava              "18.0"         ]
                   [datascript                            "0.13.3"       ] ; Latest (as of 2/1/2016)
                   [datascript-transit                    "0.2.0"        ] ; Latest (as of 5/1/2016)
                   [com.zachallaun/datomic-cljs           "0.0.1-alpha-1"] ; Latest (as of 2/1/2016)
                 ]

  :plugins [[lein-cljsbuild "1.1.1"]]

  :source-paths ["src"]
  :resource-paths ["resources" "resources-index/prod"]
  :target-path "target/%s"

  :main ^:skip-aot rente.run

  :cljsbuild
  {:builds
   {:client {:source-paths ["src/rente/client"]
             :compiler
             {:output-to "resources/public/js/app.js"
              :output-dir "dev-resources/public/js/out"}}}}

  :profiles {:dev-config {}

             :dev [:dev-config
                   {:dependencies [[org.clojure/tools.namespace "0.2.10"]
                                   [figwheel "0.5.0-2"]]

                    :plugins [[lein-figwheel "0.5.0-2"]
                              [lein-environ "1.0.1"]]

                    :source-paths ["dev"]
                    :resource-paths ^:replace
                    ["resources" "dev-resources" "resources-index/dev"]

                    :cljsbuild
                    {:builds
                     {:client {:source-paths ["dev"]
                               :compiler
                               {:optimizations :none
                                :source-map true}}}}

                    :figwheel {:http-server-root "public"
                               :port 3449
                               :repl false
                               :css-dirs ["resources/public/css"]}}]

             :prod {:cljsbuild
                    {:builds
                     {:client {:compiler
                               {:optimizations :advanced
                                :pretty-print false}}}}}}

  :aliases {"package"
            ["with-profile" "prod" "do"
             "clean" ["cljsbuild" "once"]]})
