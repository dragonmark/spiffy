(defproject spiffy "0.1.0-SNAPSHOT"
  :description "A spiffy way to start a Clojure web project"
  :url "http://spiffy-clj.org"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2268"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [http-kit "2.1.16"]
                 [compojure "1.1.8"]
                 [om "0.6.4"]]

  :jar-exclusions [#"\.cljx|\.swp|\.swo|\.DS_Store"]
  
  :source-paths ["src/cljx" "src/clj"]
  :test-paths ["target/test-classes"]

  :hooks [cljx.hooks]

  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/classes"
                   :rules :clj}

                  {:source-paths ["src/cljx"]
                   :output-path "target/classes"
                   :rules :cljs}

                  {:source-paths ["test/cljx"]
                   :output-path "target/test-classes"
                   :rules :clj}

                  {:source-paths ["test/cljx"]
                   :output-path "target/test-classes"
                   :rules :cljs}]}

  :cljsbuild {
              :builds [{:id "dev"
                        :source-paths ["target/classes" "src/cljs"]
                        :compiler {:output-to "resources/public/gen/main.js"
                                   :output-dir "resources/public/gen"
                                   :optimizations :whitespace
                                   :pretty-print true
                                   :source-map "resources/public/gen/main.js.map"
                                   }}
                       
                       {:id "release"
                        :source-paths ["target/classes" "src/cljs"]
                        :compiler {:output-to "resources/public/gen/main.js"
                                   :optimizations :advanced
                                   :pretty-print false
                                   :preamble ["react/react.min.js"]
                                   :externs ["react/externs/react.js"]}}]}

  :profiles {:dev {:plugins [[org.clojure/clojurescript "0.0-2268"]
                             [com.cemerick/clojurescript.test "0.3.0"]
                             [com.keminglabs/cljx "0.4.0"]
                             [lein-cljsbuild "1.0.3"]]
                   :aliases {"cleantest" ["do" "clean," "cljx" "once," "test,"
                                          "cljsbuild" "test"]
                             "deploy" ["do" "clean," "cljx" "once," "deploy" "clojars"]}}}

  :repositories {"sonatype-oss-public"
                 "https://oss.sonatype.org/content/repositories/snapshots/"}

  :main server
)
