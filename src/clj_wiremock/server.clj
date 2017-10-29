(ns clj-wiremock.server
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [clj-wiremock.stub :refer [->stub]]
            [clojure.tools.logging :as log])
  (:import com.github.tomakehurst.wiremock.core.WireMockConfiguration))

(defprotocol Wiremocked
  (start! [_] "Start the wiremock server.")
  (stop! [_] "Stop the wiremock server.")
  (clear! [_] "Clear down all stubs registered with the wiremock server.")
  (url [_ path] "Creates an absolute URL pointing to the wiremock server with the given path.")
  (admin-url [_ path] "Creates an absolute URL pointing to the admin endpoint of the wiremock server with the given path.")
  (scenarios [_] "Provides details on the scenarios and states currently registered with wiremock.")
  (register-stub! [_ stub-content] "Registers the given stub with wiremock."))

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
               {:body (json/generate-string (->stub stub-content))})))

(defn init-wiremock
  "Intialises a new WireMock server ready for starting on the specified port."
  [{:keys [port]}]
  (let [config (doto (new WireMockConfiguration)
                 (.port (int port)))
        wmk-java (new com.github.tomakehurst.wiremock.WireMockServer config)]
    (->WireMockServer wmk-java)))
