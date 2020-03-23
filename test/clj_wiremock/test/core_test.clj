(ns clj-wiremock.test.core-test
  (:require [clj-http.client :as http]
            [clj-wiremock.core :as wmk]
            [clj-wiremock.server :as server]
            [clj-wiremock.test.helpers :refer [ping-stub ping-url get-free-port]]
            [clojure.test :refer :all]
            [ring.util.http-predicates :as http?])
  (:import (java.net ConnectException)))

(deftest can-wrap-body-in-wiremock-startup-teardown
  (let [port (get-free-port)]
    (wmk/with-wiremock
      {:port port}

      (server/register-stub! (wmk/root-server) (ping-stub port))

      (let [{:keys [body] :as response} (http/get (ping-url port))]
        (is (= "pong" body))
        (is (http?/ok? response))))

    (is (thrown? ConnectException (http/get (ping-url port))))))

(deftest can-wrap-function-in-wiremock-fixture
  (let [port      (get-free-port)
        stub      (ping-stub port)
        test-body (fn []
                    (server/register-stub! (wmk/root-server) stub)
                    (http/get (ping-url port)))
        response  (wmk/wiremock-fixture {:port port} test-body)]
    (is (= "pong" (:body response)))
    (is (http?/ok? response))))

(deftest can-wrap-function-in-threadable-wiremock-fixture
  (let [port      (get-free-port)
        stub      (ping-stub port)
        test-body (fn []
                    (server/register-stub! (wmk/root-server) stub)
                    (http/get (ping-url port)))
        response  ((wmk/wiremock-fixture-fn {:port port} test-body))]
    (is (= "pong" (:body response)))
    (is (http?/ok? response))))

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
                        (is (http?/ok? response1))
                        (is (http?/created? response2)))))))

(deftest wiremock-is-reset-after-with-stubs-block
  (let [port (get-free-port)]
    (wmk/with-wiremock
      {:port port}

      (wmk/with-stubs
        [(ping-stub port)]
        (is (http?/ok? (http/get (ping-url port)))))

      (is (http?/not-found? (http/get (ping-url port) {:throw-exceptions? false}))))))

(deftest can-add-stub-with-explicit-wiremock-server
  (let [port (get-free-port)
        s    (server/init-wiremock {:port port})]
    (server/start! s)
    (try
      (wmk/with-stubs
        [{:req    [:GET "/ping"]
          :res    [200 {:body "pong"}]
          :server s}]

        (let [response (http/get (ping-url port))]
          (is (= "pong" (:body response)))
          (is (http?/ok? response))))

      (finally
        (server/stop! s)))))

(deftest wiremock-is-reset-after-with-explicit-server-stubs-block
  (let [port (get-free-port)
        s    (server/init-wiremock {:port port})]
    (server/start! s)
    (try
      (wmk/with-stubs
        [{:req    [:GET "/ping"]
          :res    [200 {:body "pong"}]
          :server s}]

        (is (http?/ok? (http/get (ping-url port)))))

      (is (http?/not-found? (http/get (ping-url port) {:throw-exceptions? false})))

      (finally
        (server/stop! s)))))

(deftest can-add-stub-with-explicit-wiremock-port
  (let [port (get-free-port)]
    (wmk/with-wiremock
      {:port port}

      (wmk/with-stubs
        [{:req  [:GET "/ping"]
          :res  [200 {:body "pong"}]
          :port port}]

        (let [response (http/get (ping-url port))]
          (is (= "pong" (:body response)))
          (is (http?/ok? response)))))))

(deftest wiremock-is-reset-after-with-explicit-port-stubs-block
  (let [port (get-free-port)]
    (wmk/with-wiremock
      {:port port}

      (wmk/with-stubs
        [{:req  [:GET "/ping"]
          :res  [200 {:body "pong"}]
          :port port}]

        (is (http?/ok? (http/get (ping-url port)))))
      (is (http?/not-found? (http/get (ping-url port) {:throw-exceptions? false}))))))

(deftest can-wrap-function-in-wiremocks-fixture
  (let [[port1 port2] [(get-free-port) (get-free-port)]
        stub1      (ping-stub port1)
        test-body  (fn []
                     (server/register-stub! (wmk/root-server) stub1)
                     (http/get (ping-url port1)))
        response   (wmk/wiremocks-fixture
                     [{:port port1} {:port port2}]
                     test-body)]
    (is (= "pong" (:body response)))
    (is (http?/ok? response))))