(defproject exoscale/ex "0.3.18-SNAPSHOT"
  :plugins [[lein-parent "0.3.8"]]
  :source-paths ["src/clj"]
  :parent-project {:path "../../project.clj"
                   :inherit [:managed-dependencies
                             :license
                             :url
                             :scm
                             :deploy-repositories
                             :profiles
                             :description
                             :pedantic?]}
  :dependencies [[org.clojure/clojure]])
