(ns exoscale.ex.auspex
  (:require [exoscale.ex :as ex]
            [qbits.auspex :as a]))

(defn catch
  "Like exoscale.ex/catch but with dispatch on ex style
  ExceptionInfo with arity 2"
  {:style/indent 1}
  [f type-key handler]
  (a/catch f clojure.lang.ExceptionInfo
    #(ex/catch % type-key handler (fn [e] (throw e)))))
