(ns clj-wiremock.test.sugar.stub-test
  (:require [clj-wiremock.sugar.stub :refer :all]
            [clojure.test :refer :all]))

(def ^:private dummy-method :GET)
(def ^:private dummy-status 204)
(def ^:private dummy-path "/some/path")

(deftest request-includes-specified-path
  (let [{:keys [url]} (request dummy-method "/my/path")]
    (is (= "/my/path" url))))

(deftest request-includes-specified-method
  (let [{:keys [method]} (request :DELETE dummy-path)]
    (is (= :DELETE method))))

(deftest request-method-is-upper-cased
  (let [{:keys [method]} (request :delete dummy-path)]
    (is (= :DELETE method))))

(deftest request-includes-specified-headers
  (let [{:keys [headers]} (request dummy-method dummy-path
                                   {:headers {:header1 "value1"
                                              :header2 "value2"}})]
    (is (= 2 (count headers)))
    (is (= (:header1 headers) "value1"))
    (is (= (:header2 headers) "value2"))))

(deftest request-has-all-headers-lower-cased
  (let [{:keys [headers]} (request dummy-method dummy-path
                                   {:headers {:HEADER1 "value1"}})]
    (is (= (:header1 headers) "value1"))))

(deftest request-includes-specified-body
  (let [{:keys [body]} (request dummy-method dummy-path {:body "mybody"})]
    (is (= "mybody" body))))

(deftest request-does-not-include-headers-if-not-specified
  (let [req (request dummy-method dummy-path)]
    (is (not (contains? req :headers)))))

(deftest request-does-not-include-body-if-not-specified
  (let [req (request dummy-method dummy-path)]
    (is (not (contains? req :body)))))

(deftest can-coerce-request-body-as-json
  (testing "body is coerced"
    (let [{:keys [body]} (request dummy-method dummy-path {:body {:a 1} :as :json})]
      (is (= "{\"a\":1}" body))))

  (testing "content-type header is added"
    (let [{:keys [headers]} (request dummy-method dummy-path {:body {:a 1} :as :json})]
      (is (= "application/json" (:content-type headers)))))

  (testing "content-type header is NOT added if already present"
    (let [{:keys [headers]} (request dummy-method dummy-path {:body    {:a 1}
                                                              :headers {:Content-Type "my/content-type"}
                                                              :as      :json})]
      (is (= "my/content-type" (:content-type headers))))

    (let [{:keys [headers]} (request dummy-method dummy-path {:body    {:a 1}
                                                              :headers {:content-type "my/content-type"}
                                                              :as      :json})]
      (is (= "my/content-type" (:content-type headers))))))

(deftest response-includes-specified-headers
  (let [{:keys [headers]} (response dummy-status
                                    {:headers {:header1 "value1"
                                               :header2 "value2"}})]
    (is (= 2 (count headers)))
    (is (= (:header1 headers) "value1"))
    (is (= (:header2 headers) "value2"))))

(deftest response-has-all-headers-lower-cased
  (let [{:keys [headers]} (response dummy-status
                                    {:headers {:HEADER1 "value1"}})]
    (is (= (:header1 headers) "value1"))))

(deftest response-includes-specified-body
  (let [{:keys [body]} (response dummy-status {:body "mybody"})]
    (is (= "mybody" body))))

(deftest response-does-not-include-headers-if-not-specified
  (let [resp (response dummy-status)]
    (is (not (contains? resp :headers)))))

(deftest response-does-not-include-body-if-not-specified
  (let [resp (response dummy-status)]
    (is (not (contains? resp :body)))))

(deftest can-coerce-response-body-as-json
  (testing "body is coerced"
    (let [{:keys [body]} (response dummy-status {:body {:a 1} :as :json})]
      (is (= "{\"a\":1}" body))))

  (testing "content-type header is added"
    (let [{:keys [headers]} (response dummy-status {:body {:a 1} :as :json})]
      (is (= "application/json" (:content-type headers)))))

  (testing "content-type header is NOT added if already present"
    (let [{:keys [headers]} (response dummy-status {:body    {:a 1}
                                                    :headers {:Content-Type "my/content-type"}
                                                    :as      :json})]
      (is (= "my/content-type" (:content-type headers))))

    (let [{:keys [headers]} (response dummy-status {:body    {:a 1}
                                                    :headers {:content-type "my/content-type"}
                                                    :as      :json})]
      (is (= "my/content-type" (:content-type headers))))))

(deftest stub-adds-given-request-to-map
  (let [{:keys [request]} (stub {:a 1} {:b 2})]
    (is (= {:a 1} request))))

(deftest stub-adds-given-response-to-map
  (let [{:keys [response]} (stub {:a 1} {:b 2})]
    (is (= {:b 2} response))))