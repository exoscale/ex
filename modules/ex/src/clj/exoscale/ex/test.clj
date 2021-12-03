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

(defmethod t/assert-expr 'thrown-with-ex-info-msg? [msg form]
  (let [k (nth form 1)
        re (nth form 2)
        body (nthnext form 3)]
    `(ex/try+ ~@body
              (do-report {:type :fail, :message ~msg, :expected '~form, :actual nil})
              (catch ~k d#
                (let [e# (:exoscale.ex/exception d#)
                      m# (ex-message e#)]
                  (if (re-find ~re m#)
                    (do-report {:type :pass, :message ~msg,
                                :expected '~form, :actual e#})
                    (do-report {:type :fail, :message ~msg,
                                :expected '~form, :actual e#})))
                e#))))

(defmethod t/assert-expr 'thrown-with-ex-info-cause? [msg form]
  (let [k (nth form 1)
        re (nth form 2)
        body (nthnext form 3)]
    `(ex/try+ ~@body
              (do-report {:type :fail, :message ~msg, :expected '~form, :actual nil})
              (catch ~k d#
                (let [e# (:exoscale.ex/exception d#)
                      m# (ex-cause e#)]
                  (if (re-find ~re m#)
                    (do-report {:type :pass, :message ~msg,
                                :expected '~form, :actual e#})
                    (do-report {:type :fail, :message ~msg,
                                :expected '~form, :actual e#})))
                e#))))
