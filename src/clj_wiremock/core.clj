(ns clj-wiremock.core
  (:require [clj-wiremock.server :as srv]
            [clj-wiremock.stub :refer [->stub]]))

(def wiremocks (atom {}))

(defn root-server
  "Convenience function to retrieve the first wiremock server registered - useful when only on wiremock-server is in use."
  []
  (first (vals @wiremocks)))

(defn server
  "Returns the wiremock server listening on the specified port (if any)"
  [port]
  (get @wiremocks port))

(defn wiremock-fixture
  [{:keys [port] :as config} f]
  (let [s (srv/init-wiremock config)]
    (swap! wiremocks assoc port s)
    (try
      (srv/start! s)
      (f)
      (finally
        (swap! wiremocks dissoc port)
        (srv/stop! s)))))

(defn wiremocks-fixture
  [configs f]
  (let [servers (->> configs
                     (reduce (fn [acc {:keys [port] :as c}]
                               (assoc acc port (srv/init-wiremock c)))
                             {}))]
    (swap! wiremocks merge servers)

    (try
      (doseq [[_ s] servers]
        (srv/start! s))
      (f)
      (finally
        (swap! wiremocks #(apply dissoc (cons % (keys servers))))
        (doseq [[_ s] servers]
          (srv/stop! s))))))

(defn wiremock-fixture-fn
  [config f]
  (fn []
    (wiremock-fixture config f)))

(defmacro with-wiremock
  [{:keys [port] :as config} & body]
  `(let [s# (srv/init-wiremock ~config)]
     (swap! wiremocks assoc ~port s#)
     (try
       (srv/start! s#)
       ~@body
       (finally
         (swap! wiremocks dissoc ~port)
         (srv/stop! s#)))))

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
         (srv/register-stub! (:server stub#) (dissoc stub# :server)))
       ~@body
       (finally
         (doseq [stub# stubs#]
           (srv/clear! (:server stub#)))))))

(defn request-journal
  "Pulls back the request journal for the specified wiremock server. Defaults to the root server."
  ([s] (srv/requests s))
  ([] (srv/requests (root-server))))