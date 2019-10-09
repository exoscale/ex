(load-file "../../.setup.clj")
(defproject exoscale/ex-auspex (:project/version properties)
  :profiles {:dev {:dependencies [[exoscale/ex ~(:project/version properties)]
                                  [cc.qbits/auspex "0.1.0-alpha2"]]}}
  :url "https://github.com/exoscale/ex"
  :source-paths ["src/clj"])
