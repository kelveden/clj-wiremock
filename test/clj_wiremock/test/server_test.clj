(ns clj-wiremock.test.server-test
  (:require [clj-wiremock.server :as server]
            [clj-wiremock.test.helpers :refer [ping-stub ping-url get-free-port]]
            [clojure.test :refer :all]
            [clj-http.client :as http])
  (:import (java.net ConnectException)))

(deftest can-start-and-stop-wiremock
  (let [port (get-free-port)
        wiremock (server/init-wiremock {:port port})
        ping-url (str "http://localhost:" port "/__admin/mappings")]

    (is (thrown? ConnectException (http/get ping-url)))

    (server/start! wiremock)
    (is (= 200 (:status (http/get ping-url))))
    (server/stop! wiremock)

    (is (thrown? ConnectException (http/get ping-url)))))

(deftest can-build-admin-url-from-path
  (let [port (get-free-port)
        wiremock (server/init-wiremock {:port port})]

    (try
      (server/start! wiremock)
      (is (= (str "http://localhost:" port "/__admin/some/path")
             (server/admin-url wiremock "/some/path")))
      (finally (server/stop! wiremock)))))

(deftest can-clear-stubs
  (let [port (get-free-port)
        wiremock (server/init-wiremock {:port port})]
    (try
      ; Given
      (server/start! wiremock)

      (server/register-stub! wiremock (ping-stub port))
      (is (= 200 (:status (http/get (ping-url port)))))

      ; When
      (server/clear! wiremock)

      ; Then
      (is (= 404 (:status (http/get (ping-url port) {:throw-exceptions? false}))))

      (finally (server/stop! wiremock)))))

(deftest can-retrieve-scenarios
  (let [port (get-free-port)
        wiremock (server/init-wiremock {:port port})]
    (try
      ; Given
      (server/start! wiremock)

      (server/register-stub! wiremock {:req      [:GET "/ping"]
                                       :res      [200 {:body "pong"}]
                                       :scenario "myscenario"
                                       :state    {:new "newstate"}})
      ; When
      (http/get (ping-url port))

      ; Then
      (is (clojure.set/subset? (set {:state "newstate" :name "myscenario"})
                               (set (first (server/scenarios wiremock)))))

      (finally (server/stop! wiremock)))))