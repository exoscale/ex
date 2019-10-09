(ns exoscale.ex.test.auspex-test
  (:use clojure.test)
  (:require
   [exoscale.ex :as ex]
   [exoscale.ex.auspex :as c]
   [qbits.auspex :as a]))

(deftest test-auspex
  (let [ex (ex-info "bar" {:type ::bar1 :bar :baz})]
    (is (= ::boom
           @(-> (a/error-future ex)
                (c/catch-data ::bar1
                              (fn [d] ::boom)))))

    (ex/derive ::bar1 ::baz1)
    (is (= ::boom
           @(-> (a/error-future ex)
                (c/catch-data ::baz1
                              (fn [d] ::boom)))))
    (ex/underive ::bar1 ::baz1)

    (is (= ex
           @(-> (a/error-future ex)
                (c/catch-data ::bak1
                              (fn [d] ::boom))
                (a/catch clojure.lang.ExceptionInfo (fn [e] e)))))
    (is (= :foo
           @(-> (a/success-future :foo)
                (c/catch-data ::bak1
                              (fn [d] ::boom)))))))
