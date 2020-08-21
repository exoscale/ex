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
         @(-> (d/error-deferred (ex-info "bar" {::ex/type ::bar1 :bar :baz}))
              (m/catch ::bar1
                       (fn [d] ::boom)))))

  (ex/derive ::bar1 ::baz1)
  (is (= ::boom
         @(-> (d/error-deferred (ex-info "bar" {::ex/type ::bar1 :bar :baz}))
              (m/catch ::baz1
                       (fn [d] ::boom)))))
  (ex/underive ::bar1 ::baz1)

  (is (thrown? clojure.lang.ExceptionInfo
               @(-> (d/error-deferred (ex-info "bar" {::ex/type ::bar1 :bar :baz}))
                    (m/catch ::bak1
                             (fn [d] ::boom)))))
  (is (= :foo
         @(-> (d/success-deferred :foo)
              (m/catch ::bak1
                       (fn [d] ::boom))))))

(deftest test-collisions
  (testing "make sure manifold catch is not interpreted as try+/catch"
    (is (= 1 (ex/try+
              (m/catch (d/error-deferred (ex-info "foo" {::ex/type :bar}))
                       :foo
                (fn [& args] nil))
              1
              (catch ::something e
                ::boom))))))
