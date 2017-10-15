(ns clj-wiremock.server
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [clj-wiremock.stub :refer [->stub]]
            [clojure.tools.logging :as log])
  (:import com.github.tomakehurst.wiremock.core.WireMockConfiguration))

(defprotocol Wiremocked
  (start! [_])
  (stop! [_])
  (clear! [_])
  (url [_ path])
  (admin-url [_ path])
  (register-stub! [_ stub-content]))

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
