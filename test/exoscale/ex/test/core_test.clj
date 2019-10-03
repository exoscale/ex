(ns exoscale.ex.test.core-test
  (:use clojure.test)
  (:require [exoscale.ex :as ex]
            [exoscale.ex.manifold :as m]
            [clojure.spec.alpha :as s]
            [manifold.deferred :as d]))

(defmacro try-val
  [& body]
  `(try
     ~@body
     (catch Exception e#
       e#)))

(deftest test-foo
  (let [d {:type ::foo}]
    (is (= d
           (ex/try+
               (throw (ex-info "asdf" d))
               (catch-data ::foo x
                           x))))

    (is (true?
         (ex/try+
             true
             (catch-data ::foo x
                         x))))

    ;; no match but still ex-info
    (is (= {:type ::asdf}
           (ex-data (try-val
                     (ex/try+
                         (throw (ex-info "asdf" {:type ::asdf}))
                         (catch-data ::foo x
                                     x))))))

    (is (instance? Exception
                   (try-val
                    (ex/try+
                        (throw (Exception. "boom"))
                        (catch-data ::foo x
                                    x)))))))

(deftest test-inheritance
  (ex/derive ::bar ::foo)
  (let [d {:type ::bar}]
    (is (ex/try+
            (throw (ex-info "" d))
            (catch-data ::foo ex
                        (= ex d)))))

  (ex/derive ::baz ::bar)
  (let [e {:type ::baz}]
    (is (ex/try+
            (throw (ex-info "" e))
            (catch-data ::foo ex
                        (= ex e)))))

  (let [e {:type ::bak}]
    (is (try-val (ex/try+
                     (throw (ex-info "" e))
                     (catch-data ::foo ex
                                 (= e ex)))))))

(deftest test-bindings
  (is (ex/try+
          (throw (ex-info "" {:type ::foo
                              :bar 1}))
          (catch-data ::foo {:keys [bar]}
                      (= bar 1)))))

(deftest test-complex-meta
  (let [x (ex-info "" {:type ::ex-with-meta})]
    (is (ex/try+
            (throw x)
            (catch-data ::ex-with-meta
                        x'
                        (-> x' meta ::ex/exception (= x)))))))

(deftest test-manifold
  (is (= ::boom
         @(-> (d/error-deferred (ex-info "bar" {:type ::bar1 :bar :baz}))
              (m/catch-data ::bar1
                            (fn [d] ::boom)))))

  (is (= ::boom
         @(-> (d/error-deferred (ex-info "bar" {:type ::bar1 :bar :baz}))
              (m/catch-data* ::bar1 d
                ::boom))))

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

(deftest test-spec
  (s/def ::foo string?)
  (ex/set-ex-data-spec! ::a1 (s/keys :req [::foo]))
  (is (false? (s/valid? ::ex/ex-data {:type ::a1})))
  (is (true? (s/valid? ::ex/ex-data {:type ::a1 ::foo "bar"}))))
