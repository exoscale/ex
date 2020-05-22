(defproject exoscale/ex "0.3.8-SNAPSHOT"
  :plugins [[lein-parent "0.3.8"]]
  :source-paths ["src/clj"]
  :parent-project {:path "../../project.clj"
                   :inherit [:managed-dependencies
                             :license
                             :url
                             :scm
                             :deploy-repositories
                             :description
                             :pedantic?]}
  :dependencies [[org.clojure/clojure]])
