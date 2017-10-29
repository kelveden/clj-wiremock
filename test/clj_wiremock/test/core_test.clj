(ns clj-wiremock.test.core-test
  (:require [clj-http.client :as http]
            [clj-wiremock.core :refer [*wiremock*] :as wmk]
            [clj-wiremock.server :as server]
            [clj-wiremock.test.helpers :refer [ping-stub ping-url get-free-port]]
            [clojure.test :refer :all])
  (:import (java.net ConnectException)))

(deftest can-wrap-body-in-wiremock-startup-teardown
  (let [port (get-free-port)]
    (wmk/with-wiremock
      {:port port}

      (server/register-stub! *wiremock* (ping-stub port))

      (let [{:keys [status body]} (http/get (ping-url port))]
        (is (= "pong" body))
        (is (= 200 status))))

    (is (thrown? ConnectException (http/get (ping-url port))))))

(deftest can-wrap-function-in-wiremock-fixture
  (let [port (get-free-port)
        stub (ping-stub port)
        test-body (fn []
                    (server/register-stub! *wiremock* stub)
                    (http/get (ping-url port)))
        response (wmk/wiremock-fixture {:port port} test-body)]
    (is (= "pong" (:body response)))
    (is (= 200 (:status response)))))

(deftest can-wrap-function-in-threadable-wiremock-fixture
  (let [port (get-free-port)
        stub (ping-stub port)
        test-body (fn []
                    (server/register-stub! *wiremock* stub)
                    (http/get (ping-url port)))
        response ((wmk/wiremock-fixture-fn {:port port} test-body))]
    (is (= "pong" (:body response)))
    (is (= 200 (:status response)))))

(deftest can-add-multiple-stubs
  (let [port (get-free-port)]
    (wmk/with-wiremock
      {:port port}

      (wmk/with-stubs [{:req [:GET "/ping1"] :res [200 {:body "pong1"}]}
                       {:req [:GET "/ping2"] :res [201 {:body "pong2"}]}]

                      (let [response1 (http/get (str "http://localhost:" port "/ping1"))
                            response2 (http/get (str "http://localhost:" port "/ping2"))]
                        (is (= "pong1" (:body response1)))
                        (is (= "pong2" (:body response2)))
                        (is (= 200 (:status response1)))
                        (is (= 201 (:status response2))))))))

(deftest wiremock-is-reset-after-with-stubs-block
  (let [port (get-free-port)]
    (wmk/with-wiremock
      {:port port}

      (wmk/with-stubs
        [(ping-stub port)]
        (is (= 200 (:status (http/get (ping-url port))))))

      (is (= 404 (:status (http/get (ping-url port) {:throw-exceptions? false})))))))