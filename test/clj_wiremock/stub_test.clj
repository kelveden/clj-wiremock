(ns clj-wiremock.stub-test
  (:require [clj-wiremock.stub :refer :all]
            [slingshot.test :refer :all]
            [clojure.test :refer :all]
            [clojure.spec.alpha :as s]))

(def ^:private dummy-method :GET)
(def ^:private dummy-status 204)
(def ^:private dummy-path "/some/path")
(def ^:private dummy-request [:GET dummy-path])
(def ^:private dummy-response [200])

(deftest request-includes-specified-path
  (let [{{:keys [urlPath]} :request} (->stub {:req [dummy-method "/my/path"]
                                              :res dummy-response})]
    (is (= "/my/path" urlPath))))

(deftest request-includes-specified-path-regex
  (let [{{:keys [urlPattern]} :request} (->stub {:req [dummy-method #"/my/path/.+"]
                                                 :res dummy-response})]
    (is (= "/my/path/.+" urlPattern))))

(deftest request-includes-specified-path-with-query
  (let [{{:keys [url]} :request} (->stub {:req [dummy-method "/my/path?a=b"]
                                          :res dummy-response})]
    (is (= "/my/path?a=b" url))))

(deftest request-includes-specified-method
  (let [{{:keys [method]} :request} (->stub {:req [:DELETE dummy-path]
                                             :res dummy-response})]
    (is (= :DELETE method))))

(deftest request-includes-specified-headers
  (let [{{:keys [headers]} :request} (->stub {:req [dummy-method dummy-path
                                                    {:headers {:header1 "value1"
                                                               :header2 "value2"}}]
                                              :res dummy-response})]
    (is (= 2 (count headers)))
    (is (= (:header1 headers) "value1"))
    (is (= (:header2 headers) "value2"))))

(deftest request-has-all-headers-lower-cased
  (let [{{:keys [headers]} :request} (->stub {:req [dummy-method dummy-path
                                                    {:headers {:HEADER1 "value1"}}]
                                              :res dummy-response})]
    (is (= (:header1 headers) "value1"))))

(deftest request-includes-specified-body
  (let [{{:keys [body]} :request} (->stub {:req [dummy-method dummy-path {:body "mybody"}]
                                           :res dummy-response})]
    (is (= "mybody" body))))

(deftest request-does-not-include-headers-if-not-specified
  (let [req (->stub {:req [dummy-method dummy-path]
                     :res dummy-response})]
    (is (not (contains? req :headers)))))

(deftest request-does-not-include-body-if-not-specified
  (let [req (->stub {:req [dummy-method dummy-path]
                     :res dummy-response})]
    (is (not (contains? req :body)))))

(deftest can-coerce-request-body-as-json
  (testing "body is coerced"
    (let [{{:keys [body]} :request} (->stub {:req [dummy-method dummy-path
                                                   {:body {:a 1} :as :json}]
                                             :res dummy-response})]
      (is (= "{\"a\":1}" body))))

  (testing "content-type header is added"
    (let [{{:keys [headers]} :request} (->stub {:req [dummy-method dummy-path
                                                      {:body {:a 1} :as :json}]
                                                :res dummy-response})]
      (is (= "application/json" (:content-type headers)))))

  (testing "content-type header is NOT added if already present"
    (let [{{:keys [headers]} :request} (->stub {:req [dummy-method dummy-path
                                                      {:body    {:a 1}
                                                       :headers {:Content-Type "my/content-type"}
                                                       :as      :json}]
                                                :res dummy-response})]
      (is (= "my/content-type" (:content-type headers))))

    (let [{{:keys [headers]} :request} (->stub {:req [dummy-method dummy-path
                                                      {:body    {:a 1}
                                                       :headers {:content-type "my/content-type"}
                                                       :as      :json}]
                                                :res dummy-response})]
      (is (= "my/content-type" (:content-type headers))))))

(deftest request-includes-specified-status
  (let [{{:keys [status]} :response} (->stub {:req dummy-request :res [666]})]
    (is (= 666 status))))

(deftest response-includes-specified-headers
  (let [{{:keys [headers]} :response} (->stub {:req dummy-request
                                               :res [dummy-status {:headers {:header1 "value1"
                                                                             :header2 "value2"}}]})]
    (is (= 2 (count headers)))
    (is (= (:header1 headers) "value1"))
    (is (= (:header2 headers) "value2"))))

(deftest response-has-all-headers-lower-cased
  (let [{{:keys [headers]} :response} (->stub {:req dummy-request
                                               :res [dummy-status {:headers {:HEADER1 "value1"}}]})]
    (is (= (:header1 headers) "value1"))))

(deftest response-includes-specified-body
  (let [{{:keys [body]} :response} (->stub {:req dummy-request
                                            :res [dummy-status {:body "mybody"}]})]
    (is (= "mybody" body))))

(deftest response-does-not-include-headers-if-not-specified
  (let [req (->stub {:req dummy-request
                     :res [dummy-status]})]
    (is (not (contains? req :headers)))))

(deftest response-does-not-include-body-if-not-specified
  (let [req (->stub {:req dummy-request
                     :res [dummy-status]})]
    (is (not (contains? req :body)))))

(deftest can-coerce-response-body-as-json
  (testing "body is coerced"
    (let [{{:keys [body]} :response} (->stub {:req dummy-request
                                              :res [dummy-status
                                                    {:body {:a 1} :as :json}]})]
      (is (= "{\"a\":1}" body))))

  (testing "content-type header is added"
    (let [{{:keys [headers]} :response} (->stub {:req dummy-request
                                                 :res [dummy-status
                                                       {:body {:a 1} :as :json}]})]
      (is (= "application/json" (:content-type headers)))))

  (testing "content-type header is NOT added if already present"
    (let [{{:keys [headers]} :response} (->stub {:req dummy-request
                                                 :res [dummy-status
                                                       {:body    {:a 1}
                                                        :headers {:Content-Type "my/content-type"}
                                                        :as      :json}]})]
      (is (= "my/content-type" (:content-type headers))))

    (let [{{:keys [headers]} :response} (->stub {:req dummy-request
                                                 :res [dummy-status
                                                       {:body    {:a 1}
                                                        :headers {:content-type "my/content-type"}
                                                        :as      :json}]})]
      (is (= "my/content-type" (:content-type headers))))))

(deftest stub-arguments-are-validated
  (testing "missing request causes error"
    (is (thrown+? [::s/failure :assertion-failed]
                  (->stub {:res dummy-response}))))

  (testing "missing response causes error"
    (is (thrown+? [::s/failure :assertion-failed]
                  (->stub {:req dummy-request}))))

  (testing "unrecognised request method causes error"
    (is (thrown+? [::s/failure :assertion-failed]
                  (->stub {:req [:BOLLOX dummy-path]
                           :res dummy-response}))))

  (testing "non-string request path causes error"
    (is (thrown+? [::s/failure :assertion-failed]
                  (->stub {:req [dummy-method 1234]
                           :res dummy-response}))))

  (testing "non-map request headers causes error"
    (is (thrown+? [::s/failure :assertion-failed]
                  (->stub {:req [dummy-method dummy-path {:headers "bollox"}]
                           :res dummy-response}))))

  (testing "non-numeric response status causes error"
    (is (thrown+? [::s/failure :assertion-failed]
                  (->stub {:req dummy-request
                           :res ["bollox"]}))))

  (testing "non-map response headers causes error"
    (is (thrown+? [::s/failure :assertion-failed]
                  (->stub {:req dummy-request
                           :res [200 {:headers "bollox"}]})))))

(deftest unrecognised-fields-are-left-as-is
  (testing "top-level field"
    (let [{:keys [myfield]}
          (->stub {:req     dummy-request
                   :res     dummy-response
                   :myfield "myvalue"})]
      (is (= myfield "myvalue"))))

  (testing "request field"
    (let [{{:keys [myfield]} :request}
          (->stub {:req (merge dummy-request {:myfield "myvalue"})
                   :res dummy-response})]
      (is (= myfield "myvalue"))))

  (testing "response field"
    (let [{{:keys [myfield]} :response}
          (->stub {:req dummy-request
                   :res (merge dummy-response {:myfield "myvalue"})})]
      (is (= myfield "myvalue")))))

(deftest scenario-metadata-is-added-to-stub
  (let [{:keys [requiredScenarioState newScenarioState scenarioName]}
        (->stub {:req      dummy-request
                 :res      dummy-response
                 :state    {:required "state1" :new "state2"}
                 :scenario "myscenario"})]
    (is (= "state1" requiredScenarioState))
    (is (= "state2" newScenarioState))
    (is (= "myscenario" scenarioName)))

  (testing "default scenario is used if not specified and state included"
    (let [{:keys [scenarioName]}
          (->stub {:req   dummy-request
                   :res   dummy-response
                   :state {:required "state1" :new "state2"}})]
      (is (= "__default__" scenarioName)))

    (let [{:keys [scenarioName]}
          (->stub {:req dummy-request :res dummy-response})]
      (is (nil? scenarioName))))

  (testing "required state defaults to Started if not specified "
    (let [{:keys [requiredScenarioState]}
          (->stub {:req   dummy-request :res dummy-response
                   :state {:new "somestate"}})]
      (is (= "Started" requiredScenarioState))))

  (testing "new state is optional"
    (->stub {:req   dummy-request :res dummy-response
             :state {:required "somestate"}})))