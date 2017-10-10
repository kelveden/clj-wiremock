(ns clj-wiremock.server
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure.tools.logging :as log])
  (:import com.github.tomakehurst.wiremock.core.WireMockConfiguration
           com.github.tomakehurst.wiremock.WireMockServer))

(def ^:private wiremock-server (atom nil))

(defn- wiremock-url
  [path]
  (str "http://localhost:" (.port @wiremock-server) path))

(defn- wiremock-admin-url
  [path]
  (wiremock-url (str "/__admin" path)))

(defn start-wiremock!
  "Starts wiremock listening on the specified port."
  [{:keys [port]}]
  (let [config (doto (new WireMockConfiguration)
                 (.port (int port)))
        server (new WireMockServer config)]
    (.start server)
    (reset! wiremock-server server))
  (log/info (str "Started Wiremock on port " port)))

(defn stop-wiremock!
  "Stops the wiremock server."
  []
  (when @wiremock-server
    (.stop @wiremock-server)
    (log/info (str "Wiremock stopped."))))

(defn reset-wiremock!
  "Clears down all stubs across the wiremock server."
  []
  (.resetAll @wiremock-server)
  (log/info (str "Wiremock reset")))

(defn stub [stub-content]
  (http/post (wiremock-admin-url "/mappings/new")
             {:body (json/generate-string stub-content)}))
