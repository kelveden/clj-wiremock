(defproject kelveden/clj-wiremock "0.0.2-SNAPSHOT"
  :description "Clojure bindings for WireMock"
  :url "https://github.com/kelveden/clj-wiremock"
  :license {:name "Eclipse Public License - v 1.0"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[cheshire "5.8.0"]
                 [clj-http "3.7.0"]
                 [com.github.tomakehurst/wiremock "2.8.0"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.slf4j/slf4j-api "1.7.25"]]
  :profiles {:dev {:dependencies [[org.slf4j/slf4j-simple "1.7.25"]]
                   :repl-options {:init-ns clj-wiremock.server}}})
