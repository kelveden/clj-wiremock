(ns clj-wiremock.test.examples.manually
  (:require [clj-http.client :as http]
            [clj-wiremock.server :as server]
            [clojure.test :refer :all])
  (:import (java.net ConnectException ServerSocket)))

(def ^:private wiremock-port (with-open [socket (ServerSocket. 0)]
                               (.getLocalPort socket)))
(deftest can-ping
  (let [wiremock (server/init-wiremock {:port wiremock-port})]
    (try
      (server/start! wiremock)
      (server/register-stub! wiremock {:req [:GET "/ping"] :res [200 {:body "pong"}]})

      (let [{:keys [status body]} (http/get (server/url wiremock "/ping"))]
        (is (= "pong" body))
        (is (= 200 status)))

      (finally (server/stop! wiremock)))))