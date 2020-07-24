(defproject exoscale/ex-http "0.3.12-SNAPSHOT"
  :plugins [[lein-parent "0.3.8"]]
  :parent-project {:path "../../project.clj"
                   :inherit [:managed-dependencies
                             :license
                             :url
                             :scm
                             :deploy-repositories
                             :profiles
                             :description
                             :pedantic?]}
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure]
                 [exoscale/ex]])
