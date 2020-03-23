(ns clj-wiremock.core
  (:require [clj-wiremock.server :as server]
            [clj-wiremock.stub :refer [->stub]]))

(def ^:dynamic *wiremock*)

(defn wiremock-fixture
  [config f]
  (binding [*wiremock* (server/init-wiremock config)]
    (server/start! *wiremock*)
    (try
      (f)
      (finally (server/stop! *wiremock*)))))

(defn wiremock-fixture-fn
  [config f]
  (fn []
    (wiremock-fixture config f)))

(defmacro with-wiremock
  [config & body]
  `(binding [*wiremock* (server/init-wiremock ~config)]
     (server/start! *wiremock*)
     (try
       ~@body
       (finally (server/stop! *wiremock*)))))

(defmacro with-stubs
  [stubs & body]
  `(try
     (doseq [stub# ~stubs]
       (let [server# (or (:server stub#) *wiremock*)]
         (server/register-stub! server# (dissoc stub# :server))))
     ~@body
     (finally
       (doseq [server# (->> ~stubs (map :server) (remove nil?))]
         (server/clear! server#))
       (when (bound? #'*wiremock*)
         (server/clear! *wiremock*)))))
