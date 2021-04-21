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
  [configs]
  (fn [f]
    (wiremock-fixture configs f)))

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
  ([s] (->> (srv/requests s)
            (sort-by #(get-in % [:request :loggedDate]))))
  ([] (srv/requests (root-server))))

(defn get-logged-requests
  "Get's the logged requests for the specified method and url."
  [method url & args]
  (->> (apply request-journal args)
       (filter (fn [{:keys [request]}]
                 (and (= (clojure.string/upper-case (name method))
                         (:method request))
                      (= url (:url request)))))
       (map :request)
       (vec)))