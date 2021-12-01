(defproject kelveden/clj-wiremock "1.9.0-SNAPSHOT"
  :description "Clojure bindings for WireMock"
  :url "https://github.com/kelveden/clj-wiremock"
  :license {:name "Eclipse Public License - v 1.0"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[cheshire "5.10.2"]
                 [clj-http "3.12.3"]
                 [com.github.tomakehurst/wiremock "2.27.2"]
                 [org.clojure/clojure "1.11.0"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.slf4j/slf4j-api "1.7.36"]]
  :jvm-opts ["-Dclojure.spec.check-asserts=true"]
  :profiles {:dev {:dependencies [[metosin/ring-http-response "0.9.3"]
                                  [org.slf4j/slf4j-simple "1.7.36"]
                                  [slingshot "0.12.2"]]
                   :source-paths ["dev"]
                   :repl-options {:init-ns user}}})
