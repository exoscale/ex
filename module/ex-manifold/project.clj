(load-file "../../setup.clj")
(defproject exoscale/ex-manifold (:project/version properties)
  :resource-paths ["../../resources"]
  :profiles {:dev {:dependencies [[exoscale/ex "0.1.0-SNAPSHOT"]
                                  [manifold "0.1.8"]]}}
  :url "https://github.com/exoscale/ex"
  :source-paths ["src/clj"])
