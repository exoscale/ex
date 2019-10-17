(ns exoscale.ex.test.manifold-test
  (:use clojure.test)
  (:require
   [exoscale.ex :as ex]
   [exoscale.ex.manifold :as m]
   [clojure.spec.test.alpha]
   [manifold.deferred :as d]))

(clojure.spec.test.alpha/instrument)

(deftest test-manifold
  (is (= ::boom
         @(-> (d/error-deferred (ex-info "bar" {:type ::bar1 :bar :baz}))
              (m/catch-data ::bar1
                            (fn [d] ::boom)))))

  (ex/derive ::bar1 ::baz1)
  (is (= ::boom
         @(-> (d/error-deferred (ex-info "bar" {:type ::bar1 :bar :baz}))
              (m/catch-data ::baz1
                            (fn [d] ::boom)))))
  (ex/underive ::bar1 ::baz1)

  (is (thrown? clojure.lang.ExceptionInfo
               @(-> (d/error-deferred (ex-info "bar" {:type ::bar1 :bar :baz}))
                    (m/catch-data ::bak1
                                  (fn [d] ::boom)))))
  (is (= :foo
         @(-> (d/success-deferred :foo)
              (m/catch-data ::bak1
                            (fn [d] ::boom))))))

(deftest test-collisions
  (testing "make sure manifold catch-data is not interpreted as try+/catch-data"
    (is (= 1 (ex/try+
               (m/catch-data (d/error-deferred (ex-info "foo" {:type :bar}))
                             :foo
                             (fn [& args] nil))
               1
               (catch-data ::something e
                           ::boom))))))
