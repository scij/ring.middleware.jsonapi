(ns ring.middleware.jsonapi
  (:require [jsonapi.core :as jsonapi]
            [muuntaja.core :as muuntaja]))

(def vnd-api-json-regexp #"application/vnd.api\+json")

(defn- update-if-pred
  [pred? m k f]
  (if (pred? (get m k))
    (update m k f)
    m))

(def ^{:private true} update-if-coll (partial update-if-pred coll?))

(defn wrap-request
  "Strips a Ring request body from JSON API attributes. It updates `:body` and
  `:body-params` attributes in the request thus the request body will be simple object.

  Options:
  - `:accept-regexp` - a regular expression used to detect is a response should be decorated
  - `:excluded-uris` - a set or URIs that will be excluded from decorating"
  ([handler]
   (wrap-request handler {}))
  ([handler {:keys [content-type-regexp excluded-uris]
             :as opts
             :or {content-type-regexp vnd-api-json-regexp
                  excluded-uris #{}}}]
   (fn [request]
     (let [content-type (muuntaja/extract-content-type-ring request)
           new-request (if (and (some? content-type-regexp)
                                (some? content-type)
                                (some? (re-find content-type-regexp content-type))
                                (not (contains? excluded-uris (:uri request))))
                         (-> request
                             (update-if-coll :body jsonapi/strip-body)
                             (update-if-coll :body-params jsonapi/strip-body))
                         request)]
       (handler new-request)))))

(defn wrap-response
  "Decorates a Ring response as JSON API response. It expects following keys
  in the given response:
  - `:jsonapi.core/resource-name` defines a `type` attribute of a response
  https://jsonapi.org/format/#document-resource-object-identification
  - `:jsonapi.core/id-key` defines a key name for an `id` attribute of an response
  https://jsonapi.org/format/#document-resource-object-identification
  - `:jsonapi.core/meta` an optional meta object https://jsonapi.org/format/#document-meta
  - `:jsonapi.core/links` a links object https://jsonapi.org/format/#document-links

  Options:
  - `:jsonapi` - an object that will be appended to a root of a body
  - `:accept-regexp` - a regular expression used to detect is a response should be decorated
  - `:excluded-uris` - a set or URIs that will be excluded from decorating"
  ([handler]
   (wrap-response handler {}))
  ([handler {:keys [accept-regexp excluded-uris]
             :as opts
             :or {accept-regexp vnd-api-json-regexp
                  excluded-uris #{}}}]
   (fn [request]
     (let [accept (muuntaja/extract-accept-ring request)
           {:keys [status] :as response} (handler request)]
       (if (and (number? status)
                (<= 200 status 399)
                (some? accept)
                (some? (re-find accept-regexp accept))
                (not (contains? excluded-uris (:uri request))))
         (jsonapi/decorate-response response opts)
         response)))))
