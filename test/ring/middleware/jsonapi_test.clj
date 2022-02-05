(ns ring.middleware.jsonapi-test
  (:require [clojure.test :as t]
            [jsonapi.core :as jsonapi]
            [ring.middleware.jsonapi :as m-jsonapi]))

(t/deftest wrap-response-test
  (t/testing "should work with default options"
    (t/are [expected response]
      (= expected
         (let [handler (m-jsonapi/wrap-response (constantly response))]
           (handler {:headers {"accept" "application/vnd.api+json"}})))
      {:status 200
       :headers {"Content-Type" "application/vnd.api+json"}
       :body   {:data    [{:id "1", :attributes {:name "foo", :id 1}, :type "bars"}]
                :jsonapi {:version "1.0"}}}
      {:status                 200
       :body                   [{:id 1, :name "foo"}]
       ::jsonapi/id-key        :id
       ::jsonapi/resource-name "bars"}

      {:status 200
       :headers {"Content-Type" "application/vnd.api+json"}
       :body   {:data    {:id "1", :attributes {:name "foo", :id 1}, :type "bars"},
                :jsonapi {:version "1.0"}}}
      {:status                 200
       :body                   {:id 1, :name "foo"}
       ::jsonapi/id-key        :id
       ::jsonapi/resource-name "bars"}

      {:status 200
       :headers {"Content-Type" "application/vnd.api+json"}
       :body   {:data    [{:id "1", :attributes {:name "foo", :id 1}, :type "bars"}]
                :jsonapi {:version "1.0"}
                :meta    {:total 1}}}
      {:status                 200
       :body                   [{:id 1, :name "foo"}]
       ::jsonapi/id-key        :id
       ::jsonapi/resource-name "bars"
       ::jsonapi/meta          {:total 1}}

      {:status 200
       :headers {"Content-Type" "application/vnd.api+json"}
       :body   {:data    [{:id "1" :attributes {:name "foo", :id 1}, :type "bars"}
                          {:id "2" :attributes {:name "bar", :id 2}, :type "bars"}]
                :jsonapi {:version "1.0"}
                :meta    {:total 2}}}
      {:status                 200
       :body                   [{:id 1, :name "foo"} {:id 2 :name "bar"}]
       ::jsonapi/id-key        :id
       ::jsonapi/resource-name "bars"
       ::jsonapi/meta          {:total 2}}
      ))

  (t/testing "should decorate response with a links object"
    (t/is (= {:status 201
              :headers {"Content-Type" "application/vnd.api+json"}
              :body   {:jsonapi {:version "1.0"}
                       :data
                                {:id         "1"
                                 :type       "bars"
                                 :attributes {:name "foo", :id 1}
                                 :links      {:self "http://foo"}}}}
             (let [handler (m-jsonapi/wrap-response
                             (constantly {:status                 201
                                          :body                   {:id             1
                                                                   :name           "foo"
                                                                   ::jsonapi/links {:self "http://foo"}}
                                          ::jsonapi/id-key        :id
                                          ::jsonapi/resource-name "bars"}))]
               (handler {:headers {"accept" "application/vnd.api+json"}})))))

  (t/testing "should decorate response with a relationships object"
    (t/is (= {:status 200
              :headers {"Content-Type" "application/vnd.api+json"}
              :body {:jsonapi {:version "1.0"}
                     :data
                              {:id            "1"
                               :type          "bars"
                               :attributes    {:name "foo", :id 1}
                               :relationships {:self "/bars/1/relationships/baz" :related "/bars/1/baz"}}}}
             (let [handler (m-jsonapi/wrap-response
                             (constantly {:status 200
                                          :body {:id 1
                                                 :name "foo"
                                                 ::jsonapi/relationships {:self "/bars/1/relationships/baz"
                                                                          :related "/bars/1/baz"}}
                                          ::jsonapi/id-key :id
                                          ::jsonapi/resource-name "bars"}))]
               (handler {:headers {"accept" "application/vnd.api+json"}})))))

  (t/testing "should not decorate response, error response code"
    (t/is (= {:status 400
              :body   {:id 1, :name "foo"}}
             (let [handler (m-jsonapi/wrap-response
                             (constantly {:status 400
                                          :body   {:id 1, :name "foo"}}))]
               (handler {:headers {"accept" "application/vnd.api+json"}})))))

  (t/testing "should not decorate response, no accept header"
    (t/is (= {:status 200
              :body   {:id 1, :name "foo"}}
             (let [handler (m-jsonapi/wrap-response
                             (constantly {:status 200
                                          :body   {:id 1, :name "foo"}}))]
               (handler {})))))

  (t/testing "should not decorate response, accept header doesn't match"
    (t/is (= {:status 200, :body ""}
             (let [handler (m-jsonapi/wrap-response
                             (constantly {:status 200, :body ""}))]
               (handler {:headers {"accept" "text/html"}})))))

  (t/testing "should not decorate response, excluded URI"
    (t/is (= {:status 200, :body ""}
             (let [handler (m-jsonapi/wrap-response
                             (constantly {:status 200, :body ""})
                             {:excluded-uris #{"/login"}})]
               (handler {:uri "/login"})))))

  (t/testing "should not decorate response, no response code"
    (t/is (= {:body ""}
             (let [handler (m-jsonapi/wrap-response
                             (constantly {:body ""}))]
               (handler {:uri "/login"}))))))

(t/deftest wrap-request-test
  (t/testing "should work with default options"
    (t/are [expected request]
      (let [handler (m-jsonapi/wrap-request (fn [req]
                                              (t/is (= expected req))))]
        (handler request))
      {:headers {"content-type" "application/vnd.api+json"}
       :body    {:name "foo"}}
      {:headers {"content-type" "application/vnd.api+json"}
       :body    {:data {:attributes {:name "foo"}, :type "bars"}}}

      {:headers     {"content-type" "application/vnd.api+json"}
       :body-params {:name "foo"}}
      {:headers     {"content-type" "application/vnd.api+json"}
       :body-params {:data {:attributes {:name "foo"}, :type "bars"}}}))

  (t/testing "should not work, no content type"
    (let [request {:body {:data {:attributes {:name "foo"}, :type "bars"}}}
          handler (m-jsonapi/wrap-request
                    (fn [req]
                      (t/is (= request req))))]
      (handler request)))

  (t/testing "should not work, excluded URI"
    (let [request {:uri "/bars", :body {:data {:attributes {:name "foo"}, :type "bars"}}}
          handler (m-jsonapi/wrap-request
                    (fn [req]
                      (t/is (= request req)))
                    {:excluded-uris #{"/bars"}})]
      (handler request)))

  (t/testing "should not work, no content type"
    (let [request {:body {:data {:attributes {:name "foo"}, :type "bars"}}}
          handler (m-jsonapi/wrap-request
                    (fn [req]
                      (t/is (= request req))))]
      (handler request)))

  (t/testing "should not work, no content type regular expression"
    (let [request {:headers {"content-type" "application/json"}
                   :body    {:data {:attributes {:name "foo"}, :type "bars"}}}
          handler (m-jsonapi/wrap-request
                    (fn [req]
                      (t/is (= request req)))
                    {:content-type-regexp nil})]
      (handler request))))
