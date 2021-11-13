(ns jsonapi.core-test
  (:require [clojure.test :as t]
            [jsonapi.core :as jsonapi]))

(t/deftest decorate-response-test
  (t/testing "should work with custom options"
    (t/are [expected response opts] (= expected (jsonapi/decorate-response response opts))
      {:body {:data {:attributes {:name "foo", :id 1}, :id "1", :type "bars"}, :jsonapi {:version "1.0"}}
       :headers {"Content-Type" "application/vnd.api+json"}}
      {:body {:name "foo", :id 1}, ::jsonapi/id-key :id, ::jsonapi/resource-name "bars"}
      {}

      {:body {:data {:attributes {:name "foo", :id 1}, :id "1", :type "bars"}
              :meta {:foo "bar"}, :jsonapi {:version "1.0"}}
       :headers {"Content-Type" "application/vnd.api+json"}}
      {:body {:name "foo", :id 1}
       ::jsonapi/id-key :id
       ::jsonapi/resource-name "bars"
       ::jsonapi/meta {:foo "bar"}}
      {}

      {:body {:data {:attributes {:name "foo", :id 1}, :id "1", :type "bars"}
              :meta {:foo "bar"}, :jsonapi {:version "1.0"}}
       :headers {"Content-Type" "application/vnd.api+json"}}
      {:body {:name "foo", :id 1}
       ::jsonapi/id-key :id
       ::jsonapi/resource-name "bars"
       ::jsonapi/meta {:foo "bar"}}
      {}))

  (t/testing "should work with default options"
    (t/is (= {:body {:data {:attributes {:name "foo", :id 1}, :id "1", :type "bars"}
                     :jsonapi {:version "1.0"}}
              :headers {"Content-Type" "application/vnd.api+json"}}
             (jsonapi/decorate-response
              {:body {:name "foo", :id 1}, ::jsonapi/id-key :id, ::jsonapi/resource-name "bars"})))))

(t/deftest decorate-request-test
  (t/testing "should decorate request"
    (t/is (= {:data {:attributes {:name "foo"}, :type "bars"}}
             (jsonapi/decorate-request "bars" {:name "foo"}))))

  (t/testing "should decorate request with an ID"
    (t/is (= {:data {:attributes {:name "foo", :id 1}, :type "bars", :id "1"}}
             (jsonapi/decorate-request "bars" {:name "foo", :id 1} :id)))))

(t/deftest strip-response-test
  (t/are [expected response] (= expected (jsonapi/strip-response response))
    {:body {:name "foo"}}
    {:body {:data {:attributes {:name "foo"}, :type "bars", :id "1"}}}

    {:body [{:name "foo", :id 1}]}
    {:body {:data [{:attributes {:name "foo", :id 1}, :type "bars", :id "1"}]}}))
