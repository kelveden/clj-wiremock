(defproject clj-wiremock "0.1.0-SNAPSHOT"
  :description "Clojure bindings for WireMock"
  :dependencies [[cheshire "5.8.0"]
                 [clj-http "3.7.0"]
                 [com.github.tomakehurst/wiremock "2.8.0"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.slf4j/slf4j-api "1.7.25"]]
  :profiles {:dev {:dependencies [[org.slf4j/slf4j-simple "1.7.25"]]}})
