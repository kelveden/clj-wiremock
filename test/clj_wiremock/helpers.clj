(ns clj-wiremock.helpers
  (:require [clojure.test :refer :all])
  (:import (java.net ServerSocket)))

(defn get-free-port []
  (with-open [socket (ServerSocket. 0)]
    (.getLocalPort socket)))

(defn ping-stub [port]
  {:req [:GET "/ping"]
   :res [200 {:body "pong"}]})

(defn ping-url
  [port]
  (str "http://localhost:" port "/ping"))