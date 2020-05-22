(defproject ex-parent "0.3.9-SNAPSHOT"
  :description "In which we deal with exceptions the clojure way"
  :license             {:name "MIT/ISC"}
  :url                 "https://github.com/exoscale/ex"
  :plugins [[lein-sub              "0.3.0"]
            [exoscale/lein-replace "0.1.1"]]

  :deploy-repositories [["releases" :clojars] ["snapshots" :clojars]]

  :pedantic? :warn

  :managed-dependencies [[org.clojure/clojure  "1.10.1"]
                         [manifold             "0.1.8"]
                         [cc.qbits/auspex      "0.1.0-alpha2"]
                         [exoscale/ex          :version]
                         [exoscale/ex-manifold :version]
                         [exoscale/ex-auspex   :version]]

  :sub ["modules/ex"
        "modules/ex-manifold"
        "modules/ex-auspex"]

  :release-tasks [["vcs" "assert-committed"]
                  ["sub" "change" "version" "leiningen.release/bump-version" "release"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["sub" "install"]
                  ["sub" "deploy" "clojars"]
                  ["sub" "change" "version" "leiningen.release/bump-version"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
