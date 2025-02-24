(defproject kelveden/clj-wiremock "1.9.0"
  :description "Clojure bindings for WireMock"
  :url "https://github.com/kelveden/clj-wiremock"
  :license {:name "Eclipse Public License - v 1.0"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[cheshire "5.13.0"]
                 [clj-http "3.13.0"]
                 [org.wiremock/wiremock "3.12.0"]
                 [org.clojure/clojure "1.12.0"]
                 [org.clojure/tools.logging "1.3.0"]
                 [org.slf4j/slf4j-api "2.0.16"]
                 [com.fasterxml.jackson.core/jackson-core "2.18.2"]
                 [com.fasterxml.jackson.dataformat/jackson-dataformat-cbor "2.18.2"]
                 [com.fasterxml.jackson.dataformat/jackson-dataformat-smile "2.18.2"]]
  :aliases {"test" ["run" "-m" "kaocha.runner"]
            "watch"  ["run" "-m" "kaocha.runner" "--watch"]}
  :jvm-opts ["-Dclojure.spec.check-asserts=true"]
  :profiles {:dev {:dependencies [[metosin/ring-http-response "0.9.5"]
                                  [org.slf4j/slf4j-simple "2.0.16"]
                                  [slingshot "0.12.2"]
                                  [lambdaisland/kaocha "1.91.1392"]]
                   :source-paths ["dev"]
                   :repl-options {:init-ns user}}})
