(ns clj-wiremock.test.core-test
  (:require [clj-http.client :as http]
            [clj-wiremock.core :as wmk]
            [clj-wiremock.test.helpers :refer [ping-stub ping-url get-free-port]]
            [clojure.test :refer :all])
  (:import (java.net ConnectException)))

(deftest can-wrap-body-in-wiremock-startup-teardown
  (let [port (get-free-port)
        stub (ping-stub port)]
    (wmk/with-wiremock
      {:port port}

      (wmk/stub! stub)

      (let [{:keys [status body]} (http/get (ping-url port))]
        (is (= "pong" body))
        (is (= 200 status))))

    (is (thrown? ConnectException (http/get (ping-url port))))))

(deftest can-wrap-function-in-wiremock-startup-teardown
  (let [port (get-free-port)
        stub (ping-stub port)
        response (wmk/wiremock-fixture
                   {:port port}
                   (fn []
                     (wmk/stub! stub)
                     (http/get (ping-url port))))]
    (is (= "pong" (:body response)))
    (is (= 200 (:status response)))))

(deftest can-add-multiple-stubs
  (let [port (get-free-port)]
    (wmk/with-wiremock
      {:port port}

      (wmk/with-stubs [{:request  {:method :GET :url "/ping1"}
                        :response {:status 200 :body "pong1"}}
                       {:request  {:method :GET :url "/ping2"}
                        :response {:status 201 :body "pong2"}}]

                      (let [response1 (http/get (str "http://localhost:" port "/ping1"))
                            response2 (http/get (str "http://localhost:" port "/ping2"))]
                        (is (= "pong1" (:body response1)))
                        (is (= "pong2" (:body response2)))
                        (is (= 200 (:status response1)))
                        (is (= 201 (:status response2))))))))

(deftest can-clear-stubs
  (let [port (get-free-port)
        stub (ping-stub port)]
    (wmk/with-wiremock
      {:port port}

      (wmk/stub! stub)

      (is (= 200 (:status (http/get (ping-url port)))))
      (is (= 200 (:status (http/get (ping-url port)))))

      (wmk/reset-wiremock!)

      (is (= 404 (:status (http/get (ping-url port) {:throw-exceptions? false})))))))

(deftest wiremock-reset-after-using-multiple-stubs
  (let [port (get-free-port)
        stub (ping-stub port)]
    (wmk/with-wiremock
      {:port port}

      (wmk/with-stubs
        [stub]
        (is (= 200 (:status (http/get (ping-url port))))))

      (is (= 404 (:status (http/get (ping-url port) {:throw-exceptions? false})))))))