(defproject webinar "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [org.clojure/core.async "0.4.474"]]

  :plugins [[lein-cljsbuild "1.1.7"]]

  :source-paths ["src"]

  :cljsbuild {
    :builds [{:id "webinar"
              :source-paths ["src"]
              :compiler {
                :output-to "webinar.js"
                :output-dir "out"
                :optimizations :none
                :source-map true}}]})
