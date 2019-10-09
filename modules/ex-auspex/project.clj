(load-file "../../.setup.clj")
(defproject exoscale/ex-auspex (:project/version properties)
  :dependencies [[cc.qbits/auspex "0.1.0-alpha2"]]
  :profiles {:dev {:dependencies [[exoscale/ex ~(:project/version properties)]]}}
  :url "https://github.com/exoscale/ex"
  :source-paths ["src/clj"])
