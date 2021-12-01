(ns clj-wiremock.server
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [clj-wiremock.stub :refer [->stub]]
            [clojure.tools.logging :as log])
  (:import (com.github.tomakehurst.wiremock.core WireMockConfiguration Options$ChunkedEncodingPolicy)
           (com.github.tomakehurst.wiremock.common ConsoleNotifier)
           (com.github.tomakehurst.wiremock.extension Extension)))

(defprotocol Wiremocked
  (start! [_] "Start the wiremock server.")
  (stop! [_] "Stop the wiremock server.")
  (clear! [_] "Clear down all stubs registered with the wiremock server.")
  (url [_ path] "Creates an absolute URL pointing to the wiremock server with the given path.")
  (admin-url [_ path] "Creates an absolute URL pointing to the admin endpoint of the wiremock server with the given path.")
  (scenarios [_] "Provides details on the scenarios and states currently registered with wiremock.")
  (register-stub! [_ stub-content] "Registers the given stub with wiremock.")
  (requests [_] "Returns the contents of the request journal."))

(defrecord WireMockServer [^com.github.tomakehurst.wiremock.WireMockServer wmk-java]
  Wiremocked
  (start! [_]
    (.start wmk-java)
    (log/info (str "Wiremock listening on port " (.port wmk-java))))

  (stop! [_]
    (.stop wmk-java)
    (log/info (str "Wiremock stopped.")))

  (clear! [_]
    (.resetAll wmk-java)
    (log/info (str "Wiremock reset")))

  (url [_ path]
    (str "http://localhost:" (.port wmk-java) path))

  (admin-url [_ path]
    (url _ (str "/__admin" path)))

  (scenarios [_]
    (get-in (http/get (admin-url _ "/scenarios") {:as :json}) [:body :scenarios]))

  (register-stub! [_ stub-content]
    (http/post (admin-url _ "/mappings/new")
               {:body (json/generate-string (->stub stub-content))}))

  (requests [_]
    (-> (admin-url _ "/requests")
        (http/get)
        :body
        (json/parse-string true)
        :requests)))

(defn- chunked-encoding-policy
  "Gets the ChunkedEncodingPolicy according to the given keyword."
  [policy]
  (case policy
    :always (Options$ChunkedEncodingPolicy/ALWAYS)
    :body-file (Options$ChunkedEncodingPolicy/BODY_FILE)
    :never (Options$ChunkedEncodingPolicy/NEVER)
    (throw (IllegalArgumentException. (str policy " is not a recognizable chunked encoding policy. (try :always, :body-file, or :never instead)")))))

(defn- set-wiremock-option
  "Sets the WireMock configuration according to the given key/value pair."
  [^WireMockConfiguration config key value]
  (case key
    ;; Network ports and binding
    :port (.port config value)
    :https-port (.httpsPort config (Integer/valueOf value))
    :dynamic-port? (if value (.dynamicPort config) config)
    :dynamic-https-port? (if value (.dynamicHttpsPort config) config)
    :bind-address (.bindAddress config value)

    ;; Jetty Configuration
    :container-threads (.containerThreads config (Integer/valueOf value))
    :jetty-acceptors (.jettyAcceptors config (Integer/valueOf value))
    :jetty-accept-queue-size (.jettyAcceptQueueSize config (Integer/valueOf value))
    :jetty-header-buffer-size (.jettyHeaderBufferSize config (Integer/valueOf value))
    :asynchronous-response-enabled? (.asynchronousResponseEnabled config value)
    :asynchronous-response-threads (.asynchronousResponseThreads config value)

    ;; HTTPS Configuration
    :keystore-path (.keystorePath config value)
    :keystore-password (.keystorePassword config value)
    :keystore-type (.keystoreType config value)
    :key-manager-password (.keyManagerPassword config value)
    :need-client-auth? (.needClientAuth config value)
    :trust-store-path (.trustStorePath config value)
    :trust-store-password (.trustStorePassword config value)

    ;; Proxy Settings
    :enable-browser-proxying? (.enableBrowserProxying config value)
    :preserve-host-header? (.preserveHostHeader config value)
    :proxy-host-header (.proxyHostHeader config value)
    :proxy-via (.proxyVia config (first value) (second value))
    :ca-keystore-path (.caKeystorePath config value)
    :ca-keystore-password (.caKeystorePassword config value)
    :ca-keystore-type (.caKeystoreType config value)

    ;; File Locations
    :using-files-under-directory (.usingFilesUnderDirectory config value)
    :using-files-under-classpath (.usingFilesUnderClasspath config value)

    ;; Request Journal
    :disable-request-journal? (if value (.disableRequestJournal config) config)
    :max-request-journal-entries (.maxRequestJournalEntries config value)

    ;; Notification (Logging)
    :log-to-console? (if value (.notifier config (ConsoleNotifier. true)) config)
    :notifier (.notifier config value)

    ;; Gzip
    :gzip-disabled? (.gzipDisabled config value)

    ;; Extensions - supports passing instances of Extension only at this point
    :extensions (.extensions config (into-array Extension value))

    ;; Transfer Encoding
    :use-chunked-transfer-encoding (.useChunkedTransferEncoding config (chunked-encoding-policy value))

    ;; Cross-origin response headers (CORS)
    :stub-cors-enabled? (.stubCorsEnabled config value)

    ;; Unrecognizable Option
    (throw (IllegalArgumentException. (str key " is not a recognizable clj-wiremock option.")))))

(defn- wiremock-config
  "Creates a WireMock configuration with the given options."
  [options]
  (reduce-kv set-wiremock-option (WireMockConfiguration.) options))

(defn init-wiremock
  "Intialises a new WireMock server ready for starting with the given options."
  [options]
  (let [config (wiremock-config options)
        wmk-java (com.github.tomakehurst.wiremock.WireMockServer. config)]
    (->WireMockServer wmk-java)))