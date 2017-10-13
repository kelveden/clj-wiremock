(ns clj-wiremock.sugar.stub
  (:require [cheshire.core :as json]))

(defn- coerce-body
  [{:keys [body] :as req} as]
  (case as
    :json (-> req
              (assoc :body (json/generate-string body))
              (update-in [:headers :content-type] #(if (nil? %) "application/json" %)))
    req))

(def lower-case-keyword (comp keyword clojure.string/lower-case name))

(defn- lower-case-keys-in
  [m ks]
  (if (some? (get-in m ks))
    (update-in m ks #(->> %
                          (map (fn [[k v]]
                                 [(lower-case-keyword k) v]))
                          (into {})))
    m))

(defn- strip-nils
  [m]
  (into {} (filter (comp not nil? second)) m))

(defn request
  "Syntactic sugar for building a wiremock stub request map"
  [method path & [{:keys [body as headers]}]]
  (-> {:method  (keyword (clojure.string/upper-case (name method)))
       :url     path
       :headers headers
       :body    body}
      (lower-case-keys-in [:headers])
      (coerce-body as)
      (strip-nils)))

(defn response
  "Syntactic sugar for building a wiremock stub response map"
  [status & [{:keys [body as headers]}]]
  (-> {:status  status
       :headers headers
       :body    body}
      (lower-case-keys-in [:headers])
      (coerce-body as)
      (strip-nils)))

(defn stub
  [req resp]
  {:request req :response resp})