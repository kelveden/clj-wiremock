(ns clj-wiremock.test.examples.as-fixture
  (:require [clj-http.client :as http]
            [clj-wiremock.core :refer [*wiremock*] :as wmk]
            [clj-wiremock.server :as server]
            [clj-wiremock.stub :refer [->stub]]
            [clojure.test :refer :all])
  (:import (java.net ConnectException ServerSocket)))

(def ^:private wiremock-port (with-open [socket (ServerSocket. 0)]
                               (.getLocalPort socket)))
(defn around-all
  [f]
  (wmk/wiremock-fixture {:port wiremock-port} f))

(use-fixtures :once around-all)

(deftest can-ping
  (wmk/with-stubs
    [{:req [:GET "/ping"] :res [200 {:body "pong"}]}]

    (let [{:keys [status body]} (http/get (server/url *wiremock* "/ping"))]
      (is (= "pong" body))
      (is (= 200 status)))))

(deftest can-dung
  (wmk/with-stubs
    [{:req [:GET "/ping"] :res [200 {:body "dung"}]}]

    (let [{:keys [status body]} (http/get (server/url *wiremock* "/ping"))]
      (is (= "dung" body))
      (is (= 200 status)))))