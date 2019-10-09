(ns exoscale.ex.manifold
  (:require [exoscale.ex :as ex]
            [manifold.deferred :as d]))

(defn catch-data
  "Like exoscale.ex/catch-data but with dispatch on ex style
  ExceptionInfo with arity 2"
  {:style/indent 1}
  [d type-key handler]
  (d/catch d clojure.lang.ExceptionInfo
    #(ex/catch-data* %
                     type-key
                     handler
                     d/error-deferred)))
