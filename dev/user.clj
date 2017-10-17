(ns user
  (:require [clj-wiremock.core :refer :all]
            [clj-wiremock.server :as server]
            [clj-wiremock.stub :refer [->stub]]
            [clj-http.client :as http]))

(def wmk (atom nil))

(defn start
  "Convenience function for starting a new wiremock instance listening on the specified
  port and persisting a handle to it to the wmk atom."
  [port]
  (reset! wmk (server/init-wiremock {:port port}))
  (server/start! @wmk))

(defn stop
  "Shuts down the wiremock instance referred to by the handle stored in wmk atom and clears
  that atom down."
  []
  (server/stop! @wmk)
  (reset! wmk nil))

(defn clear
  []
  (server/clear! @wmk))

(defn stub
  "Registers the specified stub with wiremock."
  [s]
  (server/register-stub! @wmk s)
  "Stub created successfully")