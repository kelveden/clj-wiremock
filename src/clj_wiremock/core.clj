(ns clj-wiremock.core
  (:require [clj-wiremock.server :as server]
            [clj-wiremock.stub :refer [->stub]]))

(def wiremocks (atom {}))

(defn root-server
  "Convenience function to retrieve the first wiremock server registered - useful when only on wiremock-server is in use."
  []
  (first (vals @wiremocks)))

(defn wiremock-fixture
  [{:keys [port] :as config} f]
  (let [s (server/init-wiremock config)]
    (swap! wiremocks assoc port s)
    (try
      (server/start! s)
      (f)
      (finally
        (swap! wiremocks dissoc port)
        (server/stop! s)))))

(defn wiremocks-fixture
  [configs f]
  (let [servers (->> configs
                     (reduce (fn [acc {:keys [port] :as c}]
                               (assoc acc port (server/init-wiremock c)))
                             {}))]
    (swap! wiremocks merge servers)

    (try
      (doseq [[_ s] servers]
        (server/start! s))
      (f)
      (finally
        (swap! wiremocks #(apply dissoc (cons % (keys servers))))
        (doseq [[_ s] servers]
          (server/stop! s))))))

(defn wiremock-fixture-fn
  [config f]
  (fn []
    (wiremock-fixture config f)))

(defmacro with-wiremock
  [{:keys [port] :as config} & body]
  `(let [s# (server/init-wiremock ~config)]
     (swap! wiremocks assoc ~port s#)
     (try
       (server/start! s#)
       ~@body
       (finally
         (swap! wiremocks dissoc ~port)
         (server/stop! s#)))))

(defmacro with-stubs
  [stubs & body]
  `(let [stubs# (->> ~stubs
                     (map
                       (fn [stub#]
                         (-> stub#
                             (assoc :server (cond
                                              (:port stub#) (get @wiremocks (:port stub#))
                                              (:server stub#) (:server stub#)
                                              :else (root-server)))
                             (dissoc :port)))))]
     (try
       (doseq [stub# stubs#]
         (server/register-stub! (:server stub#) (dissoc stub# :server)))
       ~@body
       (finally
         (doseq [stub# stubs#]
           (server/clear! (:server stub#)))))))
