(ns user
  (:require [clj-wiremock.core :refer :all]
            [clj-wiremock.server :as server]
            [clj-wiremock.sugar.stub :refer [->stub] :as sugar]
            [clj-http.client :as http]))

(def wmk (atom nil))

(defn start
  [port]
  (reset! wmk (server/init-wiremock {:port port}))
  (server/start! @wmk))

(defn stop
  []
  (server/stop! @wmk))

(defn reset
  []
  (server/clear! @wmk))

(defn new-stub
  [stub]
  (server/register-stub! @wmk stub))