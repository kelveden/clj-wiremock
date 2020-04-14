(ns clj-wiremock.stub
  (:require [cheshire.core :as json]
            [clojure.spec.alpha :as s]
            [clojure.string :refer [includes?]])
  (:import (java.util.regex Pattern)))

(def ^:private default-scenario "__default__")

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

(defn- assoc-request
  "Syntactic sugar for building a wiremock stub request map"
  [{{:keys [method opts] [url-field url-value] :path} :req
    :as stub}]
  (let [{:keys [body as headers]} opts
        request (-> opts
                    (dissoc :body :as :headers)
                    (merge {:method  (keyword (clojure.string/upper-case (name method)))
                            :headers headers
                            :body    body})
                    (assoc url-field (str url-value))
                    (lower-case-keys-in [:headers])
                    (coerce-body as)
                    (strip-nils))]
    (-> stub
        (assoc :request request)
        (dissoc :req))))

(defn- assoc-response
  "Syntactic sugar for building a wiremock stub response map"
  [{{:keys [status opts]} :res
    :as stub}]
  (let [{:keys [body as headers]} opts
        response (-> opts
                     (dissoc :body :as :headers)
                     (merge {:status  status
                             :headers headers
                             :body    body})
                     (lower-case-keys-in [:headers])
                     (coerce-body as)
                     (strip-nils))]
    (-> stub
        (assoc :response response)
        (dissoc :res))))

(defn- assoc-scenario
  [{:keys [state scenario] :as stub}]
  (-> {:scenarioName          (or scenario (when state default-scenario))
       :requiredScenarioState (or (:required state) "Started")
       :newScenarioState      (:new state)}
      (strip-nils)
      (merge stub)
      (dissoc :scenario :state)))

(defn- with-query? [url] (includes? url "?"))
(defn- not-with-query? [url] (not (with-query? url)))
(defn- pattern? [x] (= Pattern (type x)))

(s/def ::headers map?)
(s/def ::body some?)

; Request
(s/def :req/method #{:GET :POST :DELETE :PUT :TRACE :DEBUG :OPTIONS :HEAD :PATCH})
(s/def :req/opts (s/keys :opt-un [::headers ::body]))
(s/def :req/url (s/or :urlPattern pattern?
                      :urlPath (s/and string? not-with-query?)
                      :url (s/and string? with-query?)))
(s/def ::req (s/cat :method :req/method :path :req/url :opts (s/? :req/opts)))

; Response
(s/def :res/opts (s/keys :opt-un [::headers ::body]))
(s/def ::res (s/cat :status integer? :opts (s/? :res/opts)))

; Scenario/state
(s/def ::scenario string?)
(s/def :state/required string?)
(s/def :state/new string?)
(s/def ::state (s/keys :opt-un [:state/required :state/new]))

(s/def ::stub
  (s/keys :req-un [::req ::res]
          :opt-un [::state ::scenario]))

(defn ->stub
  [stub]
  {:pre [(s/assert ::stub stub)]}
  (-> (s/conform ::stub stub)
      (assoc-request)
      (assoc-response)
      (assoc-scenario)))
