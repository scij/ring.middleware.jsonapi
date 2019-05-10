(defproject ring.middleware.jsonapi "0.2.0"
  :description "A simple Ring's middleware that generates JSON API response"
  :url "https://github.com/druids/ring.middleware.jsonapi"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}

  :dependencies [[metosin/muuntaja "0.6.3"]]

  :cloverage
  {:fail-threshold 90}

  :profiles {:dev {:plugins [[lein-ancient "0.6.15"]
                             [lein-cloverage "1.0.11"]
                             [lein-kibit "0.1.6"]
                             [jonase/eastwood "0.2.5"]]

                   :dependencies [[org.clojure/clojure "1.10.0"]]}})
