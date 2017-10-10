(ns clj-wiremock.test.server-test
  (:require [clj-wiremock.core :as wmk]
            [clj-wiremock.server :as server]
            [clj-wiremock.test.helpers :refer [ping-stub ping-url get-free-port]]
            [clojure.test :refer :all]
            [clj-http.client :as http])
  (:import (java.net ConnectException)))

(deftest can-start-and-stop-wiremock-manually
  (let [port (get-free-port)
        stub (ping-stub port)]
    (try
      (server/start-wiremock! {:port port})

      (wmk/stub stub)

      (let [{:keys [status body]} (http/get (ping-url port))]
        (is (= "pong" body))
        (is (= 200 status)))
      (finally
        (server/stop-wiremock!)))

    (is (thrown? ConnectException (http/get (ping-url port))))))