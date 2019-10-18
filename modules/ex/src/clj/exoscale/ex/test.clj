(ns exoscale.ex.test
  "Testing utilities"
  (:require [clojure.test :as t]
            [exoscale.ex :as ex]))

(defmethod t/assert-expr 'thrown-ex-info-type? [msg form]
  ;; (is (thrown-ex-data? c expr))
  ;; Asserts that evaluating expr throws an exceptioninfo of :type `k`.
  ;; Returns the exceptioninfo thrown.
  (let [k (nth form 1)
        body (nthnext form 2)]
    `(ex/try+
       ~@body
       (t/do-report {:type :fail
                     :message ~msg
                     :expected '~form
                     :actual nil})
       (catch ~k d#
         (t/do-report {:type :pass
                       :message ~msg,
                       :expected '~form
                       :actual d#})
         (::ex/exception d#))
       (catch Exception e#
         (t/do-report {:type :fail
                       :message ~msg
                       :expected '~form
                       :actual e#})))))
