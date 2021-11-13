(ns jsonapi.core)

(defmulti id->str type)

(defmethod id->str #?(:clj clojure.lang.Keyword) #?(:cljs cljs.core.Keyword)
  [id]
  (name id))

(defmethod id->str :default
  [id]
  (str id))

(defn ->attribute-object
  [resource-name id-key object]
  (merge {:attributes (dissoc object ::links ::meta)}
         (when (some? id-key) {:id (id->str (get object id-key))})
         {:type resource-name}
         (reduce (fn [acc [k ns-k]]
                   (if (some? (get object ns-k))
                     (assoc acc k (get object ns-k))
                     acc))
                 {}
                 [[:links ::links] [:meta ::meta]])))

(defn- decorate-body
  [{:keys [body] :as response}]
  (let [resource-name (::resource-name response)
        id-key (::id-key response)]
    (update response
            :body
            (if (sequential? body)
              (partial reduce
                 (fn [acc object]
                   (update acc :data conj (->attribute-object resource-name id-key object)))
                 {:data []})
              #(hash-map :data (->attribute-object resource-name id-key %))))))

(defn- decorate-header [response]
  (assoc-in response [:headers "Content-Type"] "application/vnd.api+json")
  )

(defn decorate-response
  "Decorates a Ring response as JSON API response. It expects following keys
  in the given response:
  - `:jsonapi.core/resource-name` defines a `type` attribute of a response
  https://jsonapi.org/format/#document-resource-object-identification
  - `:jsonapi.core/id-key` defines a key name for an `id` attribute of an response
  https://jsonapi.org/format/#document-resource-object-identification
  - `:jsonapi.core/meta` an optional meta object https://jsonapi.org/format/#document-meta
  - `:jsonapi.core/links` a links object https://jsonapi.org/format/#document-links"
  ([response]
   (decorate-response response {}))
  ([response {:keys [jsonapi]
              :or {jsonapi {:version "1.0"}}}]
   (-> response
       decorate-header
       decorate-body
       (dissoc ::resource-name ::id-key ::meta ::links)
       (update :body merge {:jsonapi jsonapi} (when (-> response ::meta some?)
                                                {:meta (::meta response)})))))

(defn decorate-request
  "Decorates a given `object` as a JSON API request. Adds an attributes objects and `id`
  attribute if `id-key` is provided."
  ([resource-name object]
   (decorate-request resource-name object nil))
  ([resource-name object id-key]
   {:data (->attribute-object resource-name id-key object)}))

(def ^:private strip-object
  :attributes)

(defn strip-body
  [{:keys [data] :as body}]
  (if (sequential? data)
    (map strip-object data)
    (strip-object data)))

(defn strip-response
  "Converts JSON API response to a simple object with an attributes and in ID."
  [response]
  (update response :body strip-body))
