(ns exoscale.ex.test.http-test
  (:require
   [clojure.test :refer [deftest testing is] :as t]
   [exoscale.ex :as ex]
   [exoscale.ex.http :as ex-http]))

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

(deftest response->ex-info
  (testing "Should return ex/fault as the default option"
    (is (thrown-ex-info-type? ::ex/fault (ex-http/response->ex-info! {:status :not-mapped}))))

  (testing "Should return ex/not-found for a 404"
    (is (thrown-ex-info-type? ::ex/not-found (ex-http/response->ex-info! {:status 404}))))

  (testing "Should allow overwriting in a consuming namespace"
    (do (defmethod ex-http/response->ex-info! 404 [_] ::overwritten)
        (is (= ::overwritten
               (ex-http/response->ex-info! {:status 404}))))))

