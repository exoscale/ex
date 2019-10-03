(defproject exoscale/ex "0.1.0"
  :description "Yet another exception catching library"
  :url "https://github.com/exoscale/ex"
  :dependencies [[org.clojure/clojure "1.10.1"]]
  :profiles {:dev {:dependencies [[manifold "0.1.8"]]}}
  :source-paths ["src/clj"]
  :pedantic? :warn
  ;; :plugins [[lein-cljfmt "0.6.4"]] ;; messes with `:style/indent` hints
  :global-vars {*warn-on-reflection* true})
