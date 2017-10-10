(ns clj-wiremock.core
  (:require [clj-wiremock.server :as server]))

(defn with-wiremock-fn
  [config f]
  (fn []
    (try
      (server/start-wiremock! config)
      (f)
      (finally (server/stop-wiremock!)))))

(defmacro with-wiremock
  [config & body]
  `(try
     (server/start-wiremock! ~config)
     ~@body
     (finally
       (server/stop-wiremock!))))

(defmacro with-stubs
  [stubs & body]
  `(try
     (doseq [stub# ~stubs]
       (server/stub stub#))
     ~@body
     (finally
       (server/reset-wiremock!))))

(def stub server/stub)
(def reset-wiremock! server/reset-wiremock!)