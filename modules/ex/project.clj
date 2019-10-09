(load-file "../../.setup.clj")
(defproject exoscale/ex (:project/version properties)
  :description "Yet another exception catching library"
  :dependencies [[org.clojure/clojure "1.10.1"]]
  :url "https://github.com/exoscale/ex"
  :source-paths ["src/clj"]
  :pedantic? :warn)
