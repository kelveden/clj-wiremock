(ns clj-wiremock.test.server-test
  (:require [clj-wiremock.server :as server]
            [clj-wiremock.test.helpers :refer [ping-stub ping-url get-free-port]]
            [clojure.test :refer :all]
            [clj-http.client :as http])
  (:import (java.net ConnectException)
           (com.github.tomakehurst.wiremock.extension.responsetemplating ResponseTemplateTransformer)
           (com.github.tomakehurst.wiremock.core Options$ChunkedEncodingPolicy)
           (com.github.tomakehurst.wiremock.common ConsoleNotifier Slf4jNotifier Notifier)))

(deftest can-start-and-stop-wiremock
  (let [port     (get-free-port)
        wiremock (server/init-wiremock {:port port})
        ping-url (str "http://localhost:" port "/__admin/mappings")]

    (is (thrown? ConnectException (http/get ping-url)))

    (server/start! wiremock)
    (is (= 200 (:status (http/get ping-url))))
    (server/stop! wiremock)

    (is (thrown? ConnectException (http/get ping-url)))))

(deftest can-build-admin-url-from-path
  (let [port     (get-free-port)
        wiremock (server/init-wiremock {:port port})]

    (try
      (server/start! wiremock)
      (is (= (str "http://localhost:" port "/__admin/some/path")
             (server/admin-url wiremock "/some/path")))
      (finally (server/stop! wiremock)))))

(deftest can-clear-stubs
  (let [port     (get-free-port)
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
  (let [port     (get-free-port)
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

(deftest can-retrieve-request-journal
  (let [port     (get-free-port)
        wiremock (server/init-wiremock {:port port})]
    (try
      ; Given
      (server/start! wiremock)

      ; When
      (http/get (ping-url port) {:throw-exceptions false})

      ; Then
      (let [[request :as requests] (server/requests wiremock)]
        (is (= 1 (count requests)))
        (is (= "/ping" (get-in request [:request :url]))))

      (finally (server/stop! wiremock)))))

(deftest can-set-general-network-options
  (let [port 18080
        https-port 10443
        bind-address "192.0.1.99"
        wiremock (server/init-wiremock {:port         port
                                        :https-port   https-port
                                        :bind-address bind-address})]

    (let [options (-> (.getOptions (.wmk-java wiremock)))]
      (is (= port (-> options (.portNumber))))
      (is (= https-port (-> options (.httpsSettings) (.port))))
      (is (= bind-address (-> options (.bindAddress)))))))

(deftest can-set-dynamic-port-options
  (let [wiremock (server/init-wiremock {:dynamic-port?       true
                                        :dynamic-https-port? true})]

    (let [options (-> (.getOptions (.wmk-java wiremock)))]
      (is (= 0 (-> options (.portNumber))))
      (is (= 0 (-> options (.httpsSettings) (.port)))))))

(deftest do-not-set-dynamic-port-options-when-option-is-false
  (let [wiremock (server/init-wiremock {:dynamic-port?       false
                                        :dynamic-https-port? false})]

    (let [options (-> (.getOptions (.wmk-java wiremock)))]
      (is (= 8080 (-> options (.portNumber))))
      (is (= -1 (-> options (.httpsSettings) (.port)))))))

(deftest can-set-jetty-options
  (let [container-threads 11
        acceptors 22
        accept-queue-size 33
        header-buffer-size 44
        asynchronous-response-threads 55
        wiremock (server/init-wiremock {:container-threads              container-threads
                                        :jetty-acceptors                acceptors
                                        :jetty-accept-queue-size        accept-queue-size
                                        :jetty-header-buffer-size       header-buffer-size
                                        :asynchronous-response-enabled? true
                                        :asynchronous-response-threads  asynchronous-response-threads})]

    (let [options (-> (.getOptions (.wmk-java wiremock)))]
      (is (= container-threads (-> options (.containerThreads))))
      (is (= acceptors (-> options (.jettySettings) (.getAcceptors) (.get))))
      (is (= accept-queue-size (-> options (.jettySettings) (.getAcceptQueueSize) (.get))))
      (is (= header-buffer-size (-> options (.jettySettings) (.getRequestHeaderSize) (.get))))
      (is (= true (-> options (.getAsynchronousResponseSettings) (.isEnabled))))
      (is (= asynchronous-response-threads (-> options (.getAsynchronousResponseSettings) (.getThreads)))))))

(deftest can-set-https-options
  (let [keystore-password "keystore-secret"
        keystore-type "PKCS12"
        key-manager-password "key-manager-secret"
        trust-store-password "trust-store-secret"
        wiremock (server/init-wiremock {;:keystore-path        keystore-path TODO: How can we test it?
                                        :keystore-password    keystore-password
                                        :keystore-type        keystore-type
                                        :key-manager-password key-manager-password
                                        :need-client-auth?    true
                                        ;:trust-store-path     trust-store-path TODO: How can we test it?
                                        :trust-store-password trust-store-password})]

    (let [options (-> (.getOptions (.wmk-java wiremock)))]
      (is (= keystore-password (-> options (.httpsSettings) (.keyStorePassword))))
      (is (= keystore-type (-> options (.httpsSettings) (.keyStoreType))))
      (is (= key-manager-password (-> options (.httpsSettings) (.keyManagerPassword))))
      (is (= trust-store-password (-> options (.httpsSettings) (.trustStorePassword)))))))

(deftest can-set-proxy-options
  (let [proxy-via-host "my.corporate.proxy"
        proxy-host-header "my.otherdomain.com"
        proxy-via-port 8080
        ca-keystore-password "trustme"
        ca-keystore-type "JKS"
        wiremock (server/init-wiremock {:enable-browser-proxying? true
                                        :preserve-host-header?    false
                                        :proxy-host-header        proxy-host-header
                                        :ca-keystore-password     ca-keystore-password
                                        :ca-keystore-type         ca-keystore-type
                                        ;:ca-keystore-path         ca-keystore-path TODO: How can we test it?
                                        :proxy-via                [proxy-via-host proxy-via-port]})]

    (let [options (-> (.getOptions (.wmk-java wiremock)))]
      (is (= true (-> options (.browserProxyingEnabled))))
      (is (= false (-> options (.shouldPreserveHostHeader))))
      (is (= proxy-host-header (-> options (.proxyHostHeader))))
      (is (= proxy-via-host (-> options (.proxyVia) (.host))))
      (is (= proxy-via-port (-> options (.proxyVia) (.port))))
      (is (= ca-keystore-password (-> options (.browserProxySettings) (.caKeyStore) (.password))))
      (is (= ca-keystore-type (-> options (.browserProxySettings) (.caKeyStore) (.type)))))))

(deftest can-set-file-location-options-using-directory
  (let [directory "/path/to/directory"
        wiremock (server/init-wiremock {:using-files-under-directory directory})]

    (let [options (-> (.getOptions (.wmk-java wiremock)))]
      (is (= directory (-> options (.filesRoot) (.getPath)))))))

(deftest can-set-file-location-options-using-classpath
  (let [classpath "/path/into/classpath"
        wiremock (server/init-wiremock {:using-files-under-classpath classpath})]

    (let [options (-> (.getOptions (.wmk-java wiremock)))]
      (is (= classpath (-> options (.filesRoot) (.getPath)))))))

(deftest can-set-request-journal-options
  (let [max-request-journal-entries 100
        wiremock (server/init-wiremock {:disable-request-journal?    true
                                        :max-request-journal-entries max-request-journal-entries})]

    (let [options (-> (.getOptions (.wmk-java wiremock)))]
      (is (= true (-> options (.requestJournalDisabled))))
      (is (= max-request-journal-entries (-> options (.maxRequestJournalEntries) (.get)))))))

(deftest can-set-extensions-option
  (let [wiremock (server/init-wiremock {:extensions [(ResponseTemplateTransformer. true)]})]

    (let [options (-> (.getOptions (.wmk-java wiremock)))]
      (is (not (empty? (-> options (.extensionsOfType ResponseTemplateTransformer))))))))

(deftest can-log-to-console-option
  (let [wiremock (server/init-wiremock {:log-to-console? true})]

    (let [options (-> (.getOptions (.wmk-java wiremock)))]
      (is (instance? ConsoleNotifier (-> options (.notifier)))))))

(deftest do-not-set-log-to-console-option-when-false
  (let [wiremock (server/init-wiremock {:log-to-console? false})]

    (let [options (-> (.getOptions (.wmk-java wiremock)))]
      (is (instance? Slf4jNotifier (-> options (.notifier)))))))

(deftest can-set-notifier-option
  (let [notifier (reify Notifier
                   (info [_ _])
                   (error [_ _])
                   (error [_ _ _]))
        wiremock (server/init-wiremock {:notifier notifier})]

    (let [options (-> (.getOptions (.wmk-java wiremock)))]
      (is (= notifier (-> options (.notifier)))))))

(deftest can-set-chunked-transfer-encoding-option
  (let [params [[:always (Options$ChunkedEncodingPolicy/ALWAYS)]
                [:body-file (Options$ChunkedEncodingPolicy/BODY_FILE)]
                [:never (Options$ChunkedEncodingPolicy/NEVER)]]]

    (doseq [[encoding expected-policy] params]
      (let [wiremock (server/init-wiremock {:use-chunked-transfer-encoding encoding})]

        (let [options (-> (.getOptions (.wmk-java wiremock)))]
          (is (= expected-policy (-> options (.getChunkedEncodingPolicy)))))))))

(deftest cannot-set-unknown-wiremock-option
  (is (thrown-with-msg? IllegalArgumentException #":unknown is not a recognizable clj-wiremock option."
                        (server/init-wiremock {:unknown 123}))))

(deftest cannot-set-unknown-chunked-transfer-encoding-option
  (is (thrown-with-msg? IllegalArgumentException #":unknown is not a recognizable chunked encoding policy. \(try :always, :body-file, or :never instead\)"
                        (server/init-wiremock {:use-chunked-transfer-encoding :unknown}))))


(deftest can-set-stub-cors-enabled-option
  (let [wiremock (server/init-wiremock {:stub-cors-enabled? true})]

    (let [options (-> (.getOptions (.wmk-java wiremock)))]
      (is (= true (-> options (.getStubCorsEnabled)))))))