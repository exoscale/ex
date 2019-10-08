(load-file "../../setup.clj")
(defproject exoscale/ex-manifold (:project/version properties)
  :profiles {:dev {:dependencies [[exoscale/ex ~(:project/version properties)]
                                  [manifold "0.1.8"]]}}
  :url "https://github.com/exoscale/ex"
  :source-paths ["src/clj"])
