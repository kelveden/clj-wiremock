(ns clj-wiremock.test.examples.as-block
  (:require [clj-http.client :as http]
            [clj-wiremock.core :refer [*wiremock*] :as wmk]
            [clj-wiremock.server :as server]
            [clj-wiremock.stub :refer :all]
            [clojure.test :refer :all])
  (:import (java.net ConnectException ServerSocket)))

(def ^:private wiremock-port (with-open [socket (ServerSocket. 0)]
                               (.getLocalPort socket)))
(deftest can-ping
  (wmk/with-wiremock {:port wiremock-port}
    (wmk/with-stubs
      [{:req [:GET "/ping"] :res [200 {:body "pong"}]}]

      (let [{:keys [status body]} (http/get (server/url *wiremock* "/ping"))]
        (is (= "pong" body))
        (is (= 200 status))))))