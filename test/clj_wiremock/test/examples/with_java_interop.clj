(ns clj-wiremock.test.examples.with-java-interop
  (:require [clj-http.client :as http]
            [clj-wiremock.server :as server]
            [clojure.test :refer :all]
            [cheshire.core :as json])
  (:import (java.net ConnectException ServerSocket)))

(def ^:private wiremock-port (with-open [socket (ServerSocket. 0)]
                               (.getLocalPort socket)))
(deftest can-ping
  (let [wiremock (server/init-wiremock {:port wiremock-port})
        wmk-java (.wmk-java wiremock)]
    (try
      (.start wmk-java)

      (http/post (str "http://localhost:" (.port wmk-java) "/__admin/mappings")
                 {:body (json/generate-string {:request  {:method :GET :url "/ping"}
                                               :response {:status 200 :body "pong"}})})

      (let [{:keys [status body]} (http/get (str "http://localhost:" (.port wmk-java) "/ping"))]
        (is (= "pong" body))
        (is (= 200 status)))

      (finally (.stop wmk-java)))))