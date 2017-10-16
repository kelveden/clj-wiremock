(ns clj-wiremock.stub
  (:require [cheshire.core :as json]
            [clojure.spec.alpha :as s])
  (:import (java.util.regex Pattern)))

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

(defn- build-request
  "Syntactic sugar for building a wiremock stub request map"
  [{:keys [method opts] [url-field url-value] :path}]
  (let [{:keys [body as headers]} opts]
    (-> opts
        (dissoc :body :as :headers)
        (merge {:method  (keyword (clojure.string/upper-case (name method)))
                :headers headers
                :body    body})
        (assoc url-field (str url-value))
        (lower-case-keys-in [:headers])
        (coerce-body as)
        (strip-nils))))

(defn- build-response
  "Syntactic sugar for building a wiremock stub response map"
  [{:keys [status opts]}]
  (let [{:keys [body as headers]} opts]
    (-> opts
        (dissoc :body :as :headers)
        (merge {:status  status
                :headers headers
                :body    body})
        (lower-case-keys-in [:headers])
        (coerce-body as)
        (strip-nils))))

(s/def ::headers map?)
(s/def ::body some?)
(s/def ::method #{:GET :POST :DELETE :PUT :TRACE :DEBUG :OPTIONS :HEAD})
(s/def ::request-options (s/keys :opt-un [::headers ::body]))
(s/def ::url (s/or :url string? :urlPattern #(= Pattern (type %))))
(s/def ::req (s/cat :method ::method :path ::url :opts (s/? ::request-options)))

(s/def ::response-options (s/keys :opt-un [::headers ::body]))
(s/def ::res (s/cat :status integer? :opts (s/? ::response-options)))

(s/def ::stub
  (s/keys :req-un [::req ::res]))

(defn ->stub
  [stub]
  {:pre [(s/assert ::stub stub)]}
  (let [{:keys [req res] :as conformed} (s/conform ::stub stub)]
    (-> conformed
        (dissoc :req :res)
        (assoc :request (build-request req)
               :response (build-response res)))))
