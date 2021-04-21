(defproject kelveden/clj-wiremock "1.6.0"
  :description "Clojure bindings for WireMock"
  :url "https://github.com/kelveden/clj-wiremock"
  :license {:name "Eclipse Public License - v 1.0"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[cheshire "5.10.0"]
                 [clj-http "3.10.0"]
                 [com.github.tomakehurst/wiremock "2.26.3"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.logging "1.0.0"]
                 [org.slf4j/slf4j-api "1.7.30"]]
  :jvm-opts ["-Dclojure.spec.check-asserts=true"]
  :profiles {:dev {:dependencies [[metosin/ring-http-response "0.9.1"]
                                  [org.slf4j/slf4j-simple "1.7.30"]
                                  [slingshot "0.12.2"]]
                   :source-paths ["dev"]
                   :repl-options {:init-ns user}}})
