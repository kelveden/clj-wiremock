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
       (server/register-stub! *wiremock* stub#))
     ~@body
     (finally
       (server/clear! *wiremock*))))

(defn url
  [path]
  (server/url *wiremock* path))