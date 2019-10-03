(ns exoscale.ex.manifold
  (:require [exoscale.ex :as ex]
            [manifold.deferred :as d]))

(defn catch-data
  "Like exoscale.ex/catch-data but with dispatch on ex style
  ExceptionInfo with arity 2"
  [d type-key handler]
  (d/catch d clojure.lang.ExceptionInfo
    (fn [e]
      (let [d (ex-data e)]
        (ex/assert-ex-data-valid d)
        (if (ex/isa? (:type d) type-key)
          (handler (ex/data+ex d e))
          (d/error-deferred e))))))

;; Not sure I like that macro
(defmacro catch-data*
  "Like `catch-data*` but as a macro"
  {:style/indent 2}
  [d type-key binding & body]
  `(catch-data ~d ~type-key (fn [~binding] ~@body)))
