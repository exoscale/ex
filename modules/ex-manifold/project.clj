(defproject exoscale/ex-manifold "0.3.7-SNAPSHOT"
  :profiles {:dev {:dependencies [[exoscale/ex :version]
                                  [manifold "0.1.8"]]}}
  :plugins [[lein-modules "0.3.11"]]
  :modules {:parent "../../"})
