(ns exoscale.ex.manifold
  (:require [exoscale.ex :as ex]
            [manifold.deferred :as d]))

(defn catch
  "Like exoscale.ex/catch but with dispatch on ex style
  ExceptionInfo with arity 2"
  {:style/indent 1}
  [d type-key handler]
  (d/catch d clojure.lang.ExceptionInfo
    #(ex/catch % type-key handler d/error-deferred)))
