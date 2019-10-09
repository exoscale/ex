(ns exoscale.ex.auspex
  (:require [exoscale.ex :as ex]
            [qbits.auspex :as a]))

(defn catch-data
  "Like exoscale.ex/catch-data but with dispatch on ex style
  ExceptionInfo with arity 2"
  {:style/indent 1}
  [f type-key handler]
  (a/catch f clojure.lang.ExceptionInfo
    #(ex/catch-data* %
                     type-key
                     handler
                     (fn [e] (throw e)))))
